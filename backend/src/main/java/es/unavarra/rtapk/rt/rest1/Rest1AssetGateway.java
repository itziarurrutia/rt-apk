package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetFechaP;
import es.unavarra.rtapk.domain.AssetSearchField;
import es.unavarra.rtapk.domain.AssetServiceErrorException;
import es.unavarra.rtapk.rt.AssetGateway;
import es.unavarra.rtapk.rt.RtCredentialsInterceptor;
import es.unavarra.rtapk.rt.RtProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de los servicios propios de assets de RT, que responden en formato REST 1.0:
 * <ul>
 *   <li>{@code GET <assets-path>/<numero_inventario>/default}</li>
 *   <li>{@code GET <assets-path>/<numero_serie>/ns}</li>
 *   <li>{@code GET <assets-path>/<usuario_windows>/usuario}</li>
 *   <li>{@code GET <fecha-p-path>/<numero_inventario>} (campos {@code Fecha_p} y {@code Pass_iz})</li>
 * </ul>
 * No son parte de la API estándar de RT, por eso se usan también con {@code rt.api-version=rest2}.
 */
@Component
@ConditionalOnExpression("'${rt.api-version:rest1}' != 'demo'")
public class Rest1AssetGateway implements AssetGateway {

    private static final String NOT_FOUND = "No existe asset";

    private final Rest1Client client;
    private final String assetsPath;
    private final String fechaPPath;
    private final Rest1AssetMapper mapper = new Rest1AssetMapper();

    public Rest1AssetGateway(Rest1Client client, RtProperties properties) {
        this.client = client;
        this.assetsPath = properties.assetsPath();
        this.fechaPPath = properties.fechaPPath();
    }

    @Override
    public List<Asset> find(AssetSearchField field, String value) {
        String template = switch (field) {
            case INVENTARIO -> assetsPath + "/{value}/default";
            case SERIE -> assetsPath + "/{value}/ns";
            case USUARIO_WINDOWS -> assetsPath + "/{value}/usuario";
        };
        return fetch(template, value).stream().map(mapper::toAsset).toList();
    }

    @Override
    public Optional<AssetFechaP> findFechaP(String numeroInventario) {
        return fetch(fechaPPath + "/{value}", numeroInventario).stream().findFirst()
                .map(r -> new AssetFechaP(r.fields().get("Fecha_p"), r.fields().get("Pass_iz")));
    }

    /** Registros de la respuesta; vacío si el servicio contesta "# Error: No existe asset". */
    private List<Rest1Record> fetch(String template, String value) {
        Rest1Response response = client.call(c -> c.get().uri(template, value));
        if (response.hasCommentContaining(NOT_FOUND)) {
            return List.of();
        }
        // Cualquier otro "# Error: ..." es un mensaje del servicio que se muestra al usuario. Los 400/500
        // (HTTP o en la línea de estado) los convierte Rest1Client en "Request Tracker no está disponible".
        response.allComments()
                .filter(c -> c.regionMatches(true, 0, "error", 0, 5))
                .findFirst()
                .ifPresent(c -> {
                    throw new AssetServiceErrorException(errorText(c));
                });
        return response.dataRecords();
    }

    /** "Error: Base de datos no disponible" -> "Base de datos no disponible". */
    static String errorText(String comment) {
        String text = comment.substring(5).replaceFirst("^\\s*:?\\s*", "").strip();
        return text.isEmpty() ? "El servicio de equipos de RT devolvió un error sin detalle"
                : RtCredentialsInterceptor.redact(text);
    }
}
