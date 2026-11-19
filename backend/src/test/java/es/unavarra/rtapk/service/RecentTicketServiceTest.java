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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        when(rt.getAttachmentContent(20)).thenReturn("último mensaje");

        TicketsRecientes result = service.recent(AGENT);

        assertThat(result.cola()).isEqualTo("apoyo.dpto");
        assertThat(result.tickets()).containsExactly(new TicketReciente(1, "Asunto 1", "new", "david.blasco",
                "a@example.org, b@example.org", "Economia", UPDATED, "último mensaje"));
        verify(rt, never()).getAttachmentContent(10);
    }

    @Test
    void ticketWithoutPlainTextHasNoContent() {
        when(rt.findRecentActive("apoyo.dpto", "5 hours ago")).thenReturn(List.of(new TicketRef(1, "Asunto 1")));
        when(rt.getTicket(1)).thenReturn(ticket(1, " "));
        when(rt.getAttachments(1)).thenReturn(List.of(new AttachmentRef(30, "text/html")));

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
}
