package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetRequestType;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tickets de avería y de formateo abiertos desde la ficha de un equipo, en la cola
 * {@code app.assets.request-queue} (por defecto "averias"). El solicitante es el usuario autenticado (si
 * tiene correo); como en RT el ticket lo crea la cuenta de servicio, el texto se firma con el usuario real.
 */
@Service
public class AssetRequestService {

    private final AssetService assets;
    private final RtGateway rt;
    private final String queue;

    public AssetRequestService(AssetService assets, RtGateway rt,
                               @Value("${app.assets.request-queue:averias}") String queue) {
        this.assets = assets;
        this.rt = rt;
        this.queue = queue;
    }

    /** Crea el ticket y devuelve su id. */
    public long create(CurrentUser user, String numeroInventario, AssetRequestType type, String text,
                       boolean dataSaved) {
        Asset asset = assets.getByInventario(numeroInventario);
        String requestor = user.hasEmail() ? user.email() : null;
        return rt.createTicket(queue, type.subject(asset.numeroInventario()), requestor,
                body(user, asset, text, dataSaved));
    }

    String queue() {
        return queue;
    }

    static String body(CurrentUser user, Asset asset, String text, boolean dataSaved) {
        String model = Stream.of(asset.marca(), asset.modelo()).filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return text.strip()
                + "\n\nDatos salvados: " + (dataSaved ? "Sí" : "No")
                + "\nEquipo: inventario " + asset.numeroInventario()
                + (model.isBlank() ? "" : " · " + model)
                + (asset.numeroSerie() == null ? "" : " · nº de serie " + asset.numeroSerie())
                + (asset.usuarioWindows() == null ? "" : " · usuario " + asset.usuarioWindows())
                + "\n\n-- \nSolicitado por " + user.name() + " (" + user.username() + ") desde rt-apk";
    }
}
