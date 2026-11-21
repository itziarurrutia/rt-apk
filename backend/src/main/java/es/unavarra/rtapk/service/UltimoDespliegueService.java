package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.UltimoDespliegue;
import es.unavarra.rtapk.rt.RtGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Optional;

/**
 * Último despliegue de un equipo: en el historial del asset (en RT, un ticket), el mensaje más reciente con
 * contenido escrito por el usuario del script de despliegue ({@code app.assets.deployment-user}, por defecto
 * "powershell"). Es una sola consulta a RT ({@code ticket/<id>/history?format=l}).
 */
@Service
public class UltimoDespliegueService {

    /** Texto que pone RT en las transacciones sin mensaje. */
    private static final String NO_CONTENT = "This transaction appears to have no content";

    private final AssetService assets;
    private final RtGateway rt;
    private final String deploymentUser;

    public UltimoDespliegueService(AssetService assets, RtGateway rt,
                                   @Value("${app.assets.deployment-user:powershell}") String deploymentUser) {
        this.assets = assets;
        this.rt = rt;
        this.deploymentUser = deploymentUser;
    }

    public Optional<UltimoDespliegue> find(String numeroInventario) {
        Asset asset = assets.getByInventario(numeroInventario);
        return rt.getHistory(asset.id()).stream()
                .filter(e -> deploymentUser.equalsIgnoreCase(e.creator()))
                .filter(e -> hasContent(e.content()))
                .max(Comparator.comparingLong(TicketHistoryEntry::id))
                .map(e -> new UltimoDespliegue(e.id(), e.created(), e.content().strip()));
    }

    private static boolean hasContent(String content) {
        return content != null && !content.isBlank() && !content.strip().startsWith(NO_CONTENT);
    }
}
