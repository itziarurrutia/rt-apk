package es.unavarra.rtapk.service;

import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewTicketServiceTest {

    private static final CurrentUser ANA = new CurrentUser("ana.garcia", "ana.garcia@example.org", "Ana García", false);

    private final RtGateway rt = mock(RtGateway.class);
    private final AssetService assets = mock(AssetService.class);
    private final NewTicketService service = new NewTicketService(rt, assets, new NewTicketProperties(null));

    @Test
    void defaultQueuesWithTheirDescriptions() {
        assertThat(service.queues()).extracting(NewTicketProperties.QueueOption::name)
                .containsExactly("apoyo.aulas", "apoyo.si", "apoyo.dpto", "apoyo.admon", "sistemas", "averias");
        assertThat(service.queues().get(3).description()).isEqualTo("Apoyo a personal PTGAS");
    }

    @Test
    void configuredQueuesAreTrimmedAndBlankNamesIgnored() {
        NewTicketProperties props = new NewTicketProperties(List.of(
                new NewTicketProperties.QueueOption(" apoyo.x ", null),
                new NewTicketProperties.QueueOption(" ", "sin nombre")));

        assertThat(props.queues()).containsExactly(new NewTicketProperties.QueueOption("apoyo.x", ""));
    }

    @Test
    void createsUnassignedTicketAsTheUserWithInventory() {
        when(rt.createTicket(anyString(), anyString(), anyString(), any(), any(), anyString())).thenReturn(2001500L);
        when(assets.getByInventario("78763")).thenReturn(new es.unavarra.rtapk.domain.Asset(1908964, "78763", "1236718",
                "TTL", "TeknoPro Q870", null, null, null, null, null, null, "carlos.lopez"));

        long id = service.create(ANA, "apoyo.dpto", " No imprime ", "La impresora del despacho no imprime.", " 78763 ");

        assertThat(id).isEqualTo(2001500L);
        verify(rt).createTicket("apoyo.dpto", "No imprime", "ana.garcia@example.org", null,
                Map.of("Inventario", "78763"),
                "La impresora del despacho no imprime."
                        + "\n\nEquipo: inventario 78763 · TTL TeknoPro Q870 · nº de serie 1236718 · usuario carlos.lopez"
                        + "\n\n-- \nCreado por Ana García (ana.garcia) desde rt-mobile");
    }

    @Test
    void inventoryNotFoundIsStillMentioned() {
        when(assets.getByInventario("99999")).thenThrow(new es.unavarra.rtapk.domain.AssetNotFoundException("99999"));

        service.create(ANA, "apoyo.dpto", "x", "Texto", "99999");

        verify(rt).createTicket(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(Map.of("Inventario", "99999")),
                org.mockito.ArgumentMatchers.eq("Texto\n\nEquipo: inventario 99999"
                        + "\n\n-- \nCreado por Ana García (ana.garcia) desde rt-mobile"));
    }

    @Test
    void withoutInventory() {
        service.create(ANA, "apoyo.si", "Alta de cuenta", "Necesito una cuenta.", "");

        verify(rt).createTicket(org.mockito.ArgumentMatchers.eq("apoyo.si"), anyString(),
                org.mockito.ArgumentMatchers.eq("ana.garcia@example.org"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(Map.of()), anyString());
    }

    @Test
    void onlyConfiguredQueuesAndValidInventory() {
        assertThatThrownBy(() -> service.create(ANA, "general", "x", "y", null))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.create(ANA, "apoyo.dpto", "x", "y", "78763; drop"))
                .isInstanceOf(InvalidRequestException.class);
        verify(rt, never()).createTicket(anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void userWithoutEmailCannotCreateTickets() {
        CurrentUser noEmail = new CurrentUser("tecnico", null, "Técnico", true);

        assertThatThrownBy(() -> service.create(noEmail, "apoyo.dpto", "x", "y", null))
                .isInstanceOf(MissingEmailException.class);
    }
}
