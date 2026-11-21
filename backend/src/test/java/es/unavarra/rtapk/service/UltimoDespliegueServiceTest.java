package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.UltimoDespliegue;
import es.unavarra.rtapk.rt.RtGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UltimoDespliegueServiceTest {

    private static final OffsetDateTime T = OffsetDateTime.parse("2026-11-09T09:07:50+01:00");
    private static final Asset ASSET = new Asset(1908964, "78763", null, null, null, null, null, null, null, null,
            null, null);

    private final AssetService assets = mock(AssetService.class);
    private final RtGateway rt = mock(RtGateway.class);
    private final UltimoDespliegueService service = new UltimoDespliegueService(assets, rt, "powershell");

    @BeforeEach
    void setUp() {
        when(assets.getByInventario("78763")).thenReturn(ASSET);
    }

    private static TicketHistoryEntry entry(long id, String creator, String content) {
        return new TicketHistoryEntry(id, "Comment", creator, T.plusMinutes(id), "Comments added by " + creator, content);
    }

    @Test
    void returnsTheNewestMessageOfTheDeploymentUser() {
        when(rt.getHistory(1908964)).thenReturn(List.of(
                entry(10, "powershell", "Despliegue antiguo"),
                entry(30, "PowerShell", "  Ajuste Final\n- dominio  "),
                entry(40, "jlopez", "Revisado"),
                entry(50, "powershell", "This transaction appears to have no content"),
                entry(20, "powershell", "Intermedio")));

        assertThat(service.find("78763"))
                .contains(new UltimoDespliegue(30, T.plusMinutes(30), "Ajuste Final\n- dominio"));
    }

    @Test
    void assetWithoutDeployments() {
        when(rt.getHistory(1908964)).thenReturn(List.of(entry(10, "jlopez", "Revisado"), entry(11, "powershell", " ")));

        assertThat(service.find("78763")).isEmpty();
    }
}
