package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetRequestType;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

        long id = service.create(ana, "78763", AssetRequestType.AVERIA, "  No arranca.  ", true, "Windows 11");

        assertThat(id).isEqualTo(2001234L);
        verify(rt).createTicket("averias", "Avería en el equipo 78763", "ana.garcia@example.org",
                "No arranca."
                        + "\n\nEquipo: inventario 78763 · Lenovo ThinkCentre M70q · nº de serie PF4XK21B · usuario carlos.lopez"
                        + "\nDatos salvados: Sí"
                        + "\n\n-- \nSolicitado por Ana García (ana.garcia) desde rt-apk");
    }

    @Test
    void formattingRequestEndsWithDataNotSaved() {
        CurrentUser ana = new CurrentUser("ana.garcia", "ana.garcia@example.org", "Ana García", false);
        when(assets.getByInventario("78763")).thenReturn(ASSET);

        service.create(ana, "78763", AssetRequestType.FORMATEO, "Reinstalar", false, "Windows 10");

        verify(rt).createTicket(eq("averias"), eq("Formatear y poner imagen de Windows 10 en equipo: 78763"),
                eq("ana.garcia@example.org"),
                org.mockito.ArgumentMatchers.endsWith("\nSistema operativo: Windows 10\nDatos salvados: No"
                        + "\n\n-- \nSolicitado por Ana García (ana.garcia) desde rt-apk"));
    }

    @Test
    void windows11IsRejectedIfTheAssetCannotRunIt() {
        CurrentUser ana = new CurrentUser("ana.garcia", "ana.garcia@example.org", "Ana García", false);
        Asset incompatible = new Asset(1546090, "78763", null, null, null, null, null, null, null, null, null, null,
                null, null, null, "Intel Core i5-7500", null, "Windows 10", false);
        when(assets.getByInventario("78763")).thenReturn(incompatible);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.create(ana, "78763", AssetRequestType.FORMATEO, "Reinstalar", false, "Windows 11"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("no admite Windows 11");
        verify(rt, org.mockito.Mockito.never()).createTicket(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void windows11IsAllowedWithACompatibleProcessorOrIfAlreadyInstalled() {
        Asset compatible = new Asset(1, "1", null, null, null, null, null, null, null, null, null, null,
                null, null, null, "Intel Core i5-8500", "16 GB", "Windows 10", true);
        Asset installed = new Asset(2, "2", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, "Windows 11 Pro", false);

        org.assertj.core.api.Assertions.assertThat(AssetRequestService.canRunWindows11(compatible)).isTrue();
        org.assertj.core.api.Assertions.assertThat(AssetRequestService.canRunWindows11(installed)).isTrue();
        // Sin procesador indicado se deja elegir
        org.assertj.core.api.Assertions.assertThat(AssetRequestService.canRunWindows11(ASSET)).isTrue();
    }

    @Test
    void formattingNeedsAValidOperatingSystem() {
        CurrentUser ana = new CurrentUser("ana.garcia", "ana.garcia@example.org", "Ana García", false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.create(ana, "78763", AssetRequestType.FORMATEO, "Reinstalar", false, "Windows 7"))
                .isInstanceOf(InvalidRequestException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.create(ana, "78763", AssetRequestType.FORMATEO, "Reinstalar", false, null))
                .isInstanceOf(InvalidRequestException.class);
        verify(rt, org.mockito.Mockito.never()).createTicket(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void userWithoutEmailCannotOpenTickets() {
        CurrentUser tecnico = new CurrentUser("tecnico", null, "Técnico", true);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.create(tecnico, "78763", AssetRequestType.AVERIA, "No arranca", true, null))
                .isInstanceOf(MissingEmailException.class);
        verify(rt, org.mockito.Mockito.never()).createTicket(anyString(), anyString(), anyString(), anyString());
    }
}
