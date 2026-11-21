package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketRef;
import es.unavarra.rtapk.domain.TicketReciente;
import es.unavarra.rtapk.domain.TicketsRecientes;
import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecentTicketServiceTest {

    private static final CurrentUser AGENT = new CurrentUser("tecnico", "tecnico@example.org", "Técnico", true);
    private static final OffsetDateTime UPDATED = OffsetDateTime.parse("2026-11-09T09:07:50+01:00");

    private final RtGateway rt = mock(RtGateway.class);
    private final RecentTicketService service = new RecentTicketService(rt, "apoyo.dpto", "5 hours ago");

    private static Ticket ticket(long id, String departamento) {
        return new Ticket(id, "Asunto " + id, "apoyo.dpto", "new", "david.blasco",
                List.of("a@example.org", "b@example.org"), 10, UPDATED, UPDATED,
                departamento == null ? Map.of() : Map.of("NDepartamento", departamento));
    }

    @Test
    void combinesTicketDataWithLastPlainTextAttachment() {
        when(rt.findRecentActive("apoyo.dpto", "5 hours ago")).thenReturn(List.of(new TicketRef(1, "Asunto 1")));
        when(rt.getTicket(1)).thenReturn(ticket(1, "Economia"));
        when(rt.getAttachments(1)).thenReturn(List.of(
                new AttachmentRef(10, "text/plain"),
                new AttachmentRef(30, "text/html"),
                new AttachmentRef(20, "TEXT/PLAIN"),
                new AttachmentRef(25, "multipart/alternative")));
        // RT 6 guarda en text/html los mensajes escritos desde su web: cuenta el de mayor id (30)
        when(rt.getAttachmentContent(30)).thenReturn("<p>Hola,</p><p>no imprime&nbsp;&amp; no escanea.<br>Gracias</p>");

        TicketsRecientes result = service.recent(AGENT);

        assertThat(result.cola()).isEqualTo("apoyo.dpto");
        assertThat(result.tickets()).containsExactly(new TicketReciente(1, "Asunto 1", "new", "david.blasco",
                "a@example.org, b@example.org", "Economia", UPDATED, "Hola,\nno imprime & no escanea.\nGracias"));
        verify(rt, never()).getAttachmentContent(10);
        verify(rt, never()).getAttachmentContent(20);
    }

    @Test
    void emailWithPlainAndHtmlPartsUsesThePlainOne() {
        when(rt.findRecentActive("apoyo.dpto", "5 hours ago")).thenReturn(List.of(new TicketRef(1, "Asunto 1")));
        when(rt.getTicket(1)).thenReturn(ticket(1, "Economia"));
        when(rt.getAttachments(1)).thenReturn(List.of(
                new AttachmentRef(30, "text/html"),
                new AttachmentRef(40, "multipart/alternative"),
                new AttachmentRef(41, "text/plain"),
                new AttachmentRef(42, "text/html")));
        when(rt.getAttachmentContent(41)).thenReturn("respuesta por correo");

        assertThat(service.recent(AGENT).tickets().getFirst().contenido()).isEqualTo("respuesta por correo");
        verify(rt, never()).getAttachmentContent(42);
    }

    @Test
    void plainTextContentIsKeptAsIs() {
        when(rt.findRecentActive("apoyo.dpto", "5 hours ago")).thenReturn(List.of(new TicketRef(1, "Asunto 1")));
        when(rt.getTicket(1)).thenReturn(ticket(1, "Economia"));
        when(rt.getAttachments(1)).thenReturn(List.of(new AttachmentRef(16028656, "text/html")));
        when(rt.getAttachmentContent(16028656)).thenReturn("Hay problemas con la impresora. 2 < 3");

        assertThat(service.recent(AGENT).tickets().getFirst().contenido()).isEqualTo("Hay problemas con la impresora. 2 < 3");
    }

    @Test
    void ticketWithoutMessageHasNoContent() {
        when(rt.findRecentActive("apoyo.dpto", "5 hours ago")).thenReturn(List.of(new TicketRef(1, "Asunto 1")));
        when(rt.getTicket(1)).thenReturn(ticket(1, " "));
        when(rt.getAttachments(1)).thenReturn(List.of(new AttachmentRef(30, "image/png")));

        TicketReciente t = service.recent(AGENT).tickets().getFirst();

        assertThat(t.contenido()).isNull();
        assertThat(t.departamento()).isNull();
    }

    @Test
    void oneFailingTicketDoesNotBreakTheList() {
        when(rt.findRecentActive("apoyo.dpto", "5 hours ago"))
                .thenReturn(List.of(new TicketRef(1, "Asunto 1"), new TicketRef(2, "Asunto 2")));
        when(rt.getTicket(1)).thenThrow(new RtAccessException("timeout"));
        when(rt.getTicket(2)).thenReturn(ticket(2, "Economia"));
        when(rt.getAttachments(2)).thenReturn(List.of());

        List<TicketReciente> tickets = service.recent(AGENT).tickets();

        // Se mantiene el orden de la búsqueda; el que falla conserva al menos id y asunto
        assertThat(tickets).extracting(TicketReciente::id).containsExactly(1L, 2L);
        assertThat(tickets.getFirst()).isEqualTo(new TicketReciente(1, "Asunto 1", null, null, null, null, null, null));
        assertThat(tickets.get(1).departamento()).isEqualTo("Economia");
    }

    @Test
    void onlyForAgents() {
        CurrentUser user = new CurrentUser("ana", "ana@example.org", "Ana", false);

        assertThatThrownBy(() -> service.recent(user)).isInstanceOf(AccessDeniedException.class);
        verify(rt, never()).findRecentActive("apoyo.dpto", "5 hours ago");
    }

    private static final String PTGAS = "Queue = 'apoyo.admon' AND Created > '2 hours ago' AND Owner = '{usuario}' AND Status = '__Active__'";
    private static final String INGENIERIA = "Created > '2 hours ago' AND Status = '__Active__' AND Queue = 'apoyo.dpto' AND CF.{NDepartamento} = 'Ingenieria'";

    private final RecentTicketService byRole = new RecentTicketService(rt, "apoyo.dpto", "5 hours ago",
            new TicketQueriesProperties(Map.of("ptgas", PTGAS, "ingenieria", INGENIERIA),
                    Map.of("ptgas", "asignados a mí")));

    private static CurrentUser withRoles(boolean agent, String... roles) {
        return new CurrentUser("usuario", null, "Usuario", agent, Set.of(roles));
    }

    @Test
    void usesTheQueryOfTheUsersRole() {
        when(rt.findByQuery(PTGAS.replace("{usuario}", "usuario"))).thenReturn(List.of(new TicketRef(7, "Asunto 7")));
        when(rt.getTicket(7)).thenReturn(ticket(7, null));
        when(rt.getAttachments(7)).thenReturn(List.of());

        TicketsRecientes result = byRole.recent(withRoles(false, "ptgas", "default-roles-rt-mobile"));

        assertThat(result.perfiles()).containsExactly("ptgas");
        assertThat(result.descripciones()).containsExactly(Map.entry("ptgas", "asignados a mí"));
        assertThat(result.cola()).isNull();
        assertThat(result.tickets()).extracting(TicketReciente::id).containsExactly(7L);
        verify(rt, never()).findRecentActive(anyString(), anyString());
    }

    @Test
    void combinesSeveralRolesWithOr() {
        when(rt.findByQuery("(" + INGENIERIA + ") OR (" + PTGAS.replace("{usuario}", "usuario") + ")"))
                .thenReturn(List.of());

        TicketsRecientes result = byRole.recent(withRoles(true, "PTGAS", "ingenieria"));

        // Roles sin distinguir mayúsculas y en orden alfabético
        assertThat(result.perfiles()).containsExactly("ingenieria", "ptgas");
        // Sin texto configurado, "activos"
        assertThat(result.descripciones()).containsExactly(
                Map.entry("ingenieria", "activos"), Map.entry("ptgas", "asignados a mí"));
        assertThat(result.tickets()).isEmpty();
    }

    @Test
    void agentsWithoutProfileUseTheDefaultQuery() {
        when(rt.findRecentActive("apoyo.dpto", "5 hours ago")).thenReturn(List.of());

        TicketsRecientes result = byRole.recent(withRoles(true, "rt-agent"));

        assertThat(result.cola()).isEqualTo("apoyo.dpto");
        assertThat(result.perfiles()).isEmpty();
        verify(rt, never()).findByQuery(anyString());
    }

    @Test
    void usersWithoutProfileOrAgentRoleAreRejected() {
        assertThatThrownBy(() -> byRole.recent(withRoles(false, "otro"))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void profileIncludesTicketsReturnedByItsQuery() {
        String ptgas = PTGAS.replace("{usuario}", "usuario");
        when(rt.findByQuery("(" + ptgas + ") AND id = 7")).thenReturn(List.of(new TicketRef(7, "Asunto 7")));

        assertThat(byRole.profileIncludes(withRoles(false, "ptgas"), 7)).isTrue();
        assertThat(byRole.profileIncludes(withRoles(false, "ptgas"), 8)).isFalse();
    }

    @Test
    void usersWithoutProfileIncludeNoTickets() {
        assertThat(byRole.profileIncludes(withRoles(false, "otro"), 7)).isFalse();
        verify(rt, never()).findByQuery(anyString());
    }

    @Test
    void insertsTheAuthenticatedUserEscaped() {
        CurrentUser user = new CurrentUser("o'brien", "ob@example.org", "O'Brien", false, Set.of("ptgas"));

        assertThat(byRole.queryFor("ptgas", user)).contains("Owner = 'o\\'brien'");
    }

    @Test
    void emailPlaceholderNeedsAnEmail() {
        RecentTicketService service = new RecentTicketService(rt, "apoyo.dpto", "5 hours ago",
                new TicketQueriesProperties(Map.of("solicitante", "Requestor = '{email}'")));

        assertThat(service.queryFor("solicitante", new CurrentUser("ana", "ana@example.org", "Ana", false, Set.of())))
                .isEqualTo("Requestor = 'ana@example.org'");
        assertThatThrownBy(() -> service.queryFor("solicitante", new CurrentUser("ana", null, "Ana", false, Set.of())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void mineReturnsTheFiveNewestActiveTicketsOwnedByTheUser() {
        CurrentUser ana = new CurrentUser("ana.o'neil", null, "Ana", false);
        List<TicketRef> refs = java.util.stream.LongStream.rangeClosed(1, 7)
                .mapToObj(id -> new TicketRef(id, "Asunto " + id)).toList();
        when(rt.findByQuery("Status = '__Active__' AND Owner = 'ana.o\\'neil'", "-Created")).thenReturn(refs);
        when(rt.getTicket(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(inv -> ticket(inv.getArgument(0), null));

        List<TicketReciente> mine = service.mine(ana);

        assertThat(mine).extracting(TicketReciente::id).containsExactly(1L, 2L, 3L, 4L, 5L);
        verify(rt, never()).getTicket(6);
    }
}
