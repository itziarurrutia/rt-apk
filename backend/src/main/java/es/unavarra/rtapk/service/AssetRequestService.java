package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetRequestType;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
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

    /** Sistemas operativos que se pueden pedir al solicitar un formateo. */
    public static final List<String> OPERATING_SYSTEMS = List.of("Windows 10", "Windows 11");
    private static final String WINDOWS_11 = "Windows 11";

    private final AssetService assets;
    private final RtGateway rt;
    private final String queue;

    public AssetRequestService(AssetService assets, RtGateway rt,
                               @Value("${app.assets.request-queue:averias}") String queue) {
        this.assets = assets;
        this.rt = rt;
        this.queue = queue;
    }

    /**
     * Crea el ticket y devuelve su id. En un formateo, {@code operatingSystem} (uno de {@link #OPERATING_SYSTEMS})
     * es obligatorio y va en el texto; en una avería se ignora.
     */
    public long create(CurrentUser user, String numeroInventario, AssetRequestType type, String text,
                       boolean dataSaved, String operatingSystem) {
        String os = type == AssetRequestType.FORMATEO ? validOperatingSystem(operatingSystem) : null;
        // El solicitante es siempre el correo del usuario: RT le enviará las respuestas
        if (!user.hasEmail()) {
            throw new MissingEmailException(
                    "Tu usuario no tiene correo electrónico en Keycloak: no se puede abrir el ticket a tu nombre");
        }
        Asset asset = assets.getByInventario(numeroInventario);
        if (WINDOWS_11.equals(os) && !canRunWindows11(asset)) {
            throw new InvalidRequestException("El equipo " + asset.numeroInventario() + " no admite Windows 11");
        }
        return rt.createTicket(queue, type.subject(asset.numeroInventario(), os), user.email(),
                body(user, asset, text, dataSaved, os));
    }

    /**
     * Puede llevar Windows 11 si el procesador es compatible, si no se conoce (se deja elegir) o si ya lo tiene
     * instalado. Solo se descarta con un procesador conocido y no compatible.
     */
    static boolean canRunWindows11(Asset asset) {
        String current = asset.sistemaOperativo() == null ? "" : asset.sistemaOperativo().strip().toLowerCase(Locale.ROOT);
        boolean processorUnknown = asset.procesador() == null || asset.procesador().isBlank();
        return asset.compatibleWindows11() || processorUnknown || current.startsWith("windows 11");
    }

    private static String validOperatingSystem(String value) {
        String os = value == null ? "" : value.strip();
        if (!OPERATING_SYSTEMS.contains(os)) {
            throw new InvalidRequestException("Elige el sistema operativo del formateo: " + String.join(" o ", OPERATING_SYSTEMS));
        }
        return os;
    }

    String queue() {
        return queue;
    }

    static String body(CurrentUser user, Asset asset, String text, boolean dataSaved, String operatingSystem) {
        String model = Stream.of(asset.marca(), asset.modelo()).filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        // "Datos salvados" va al final del texto, justo antes de la firma
        return text.strip()
                + "\n\nEquipo: inventario " + asset.numeroInventario()
                + (model.isBlank() ? "" : " · " + model)
                + (asset.numeroSerie() == null ? "" : " · nº de serie " + asset.numeroSerie())
                + (asset.usuarioWindows() == null ? "" : " · usuario " + asset.usuarioWindows())
                + (operatingSystem == null ? "" : "\nSistema operativo: " + operatingSystem)
                + "\nDatos salvados: " + (dataSaved ? "Sí" : "No")
                + "\n\n-- \nSolicitado por " + user.name() + " (" + user.username() + ") desde rt-mobile";
    }
}
