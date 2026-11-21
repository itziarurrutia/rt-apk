package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetRequestType;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetRequestServiceTest {

    private static final Asset ASSET = new Asset(1546090, "78763", "PF4XK21B", "Lenovo", "ThinkCentre M70q",
            null, null, null, null, null, null, "carlos.lopez");

    private final AssetService assets = mock(AssetService.class);
    private final RtGateway rt = mock(RtGateway.class);
    private final AssetRequestService service = new AssetRequestService(assets, rt, "averias");

    @Test
    void createsFaultTicketInTheConfiguredQueue() {
        CurrentUser ana = new CurrentUser("ana.garcia", "ana.garcia@example.org", "Ana García", false);
        when(assets.getByInventario("78763")).thenReturn(ASSET);
        when(rt.createTicket(eq("averias"), anyString(), anyString(), anyString())).thenReturn(2001234L);

        long id = service.create(ana, "78763", AssetRequestType.AVERIA, "  No arranca.  ", true);

        assertThat(id).isEqualTo(2001234L);
        verify(rt).createTicket("averias", "Avería en el equipo 78763", "ana.garcia@example.org",
                "No arranca.\n\nDatos salvados: Sí"
                        + "\nEquipo: inventario 78763 · Lenovo ThinkCentre M70q · nº de serie PF4XK21B · usuario carlos.lopez"
                        + "\n\n-- \nSolicitado por Ana García (ana.garcia) desde rt-apk");
    }

    @Test
    void formattingRequestWithoutEmailHasNoRequestor() {
        CurrentUser tecnico = new CurrentUser("tecnico", null, "Técnico", true);
        when(assets.getByInventario("78763")).thenReturn(ASSET);

        service.create(tecnico, "78763", AssetRequestType.FORMATEO, "Reinstalar", false);

        verify(rt).createTicket(eq("averias"), eq("Solicitud de formateo del equipo 78763"), isNull(),
                org.mockito.ArgumentMatchers.contains("Datos salvados: No"));
    }
}
