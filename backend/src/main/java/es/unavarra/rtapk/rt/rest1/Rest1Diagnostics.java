package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Comprueba que responden las rutas de RT REST 1.0 que usa la aplicación (solo lectura, con datos que no
 * existen: basta con que RT conteste). Sirve para distinguir un fallo de la aplicación de uno de RT, por
 * ejemplo un servicio propio de la UPNA (asset, fecha_p) que ha dejado de estar desplegado. Nunca muestra
 * las credenciales: los errores llegan ya sin la contraseña desde {@link Rest1Client}.
 */
@Component
@ConditionalOnExpression("'${rt.api-version:rest1}' != 'demo'")
public class Rest1Diagnostics {

    /** Resultado de una ruta: si RT ha respondido y, si no, un resumen del error. */
    public record Check(String name, String path, boolean ok, String detail) {
    }

    private static final int MAX_DETAIL = 160;

    private final Rest1Client client;
    private final RtProperties properties;

    public Rest1Diagnostics(Rest1Client client, RtProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<Check> run() {
        return List.of(
                check("Búsqueda de tickets", "/search/ticket?query=id = 1",
                        c -> c.get().uri(b -> b.path("/search/ticket").queryParam("query", "{q}").build("id = 1"))),
                check("Ticket", "/ticket/1/show"),
                check("Historial de ticket", "/ticket/1/history"),
                check("Equipos por inventario", properties.assetsPath() + "/00000/default"),
                check("Equipos por nº de serie", properties.assetsPath() + "/00000/ns"),
                check("Equipos por usuario", properties.assetsPath() + "/rtmobile-diagnostico/usuario"),
                check("Fecha P", properties.fechaPPath() + "/00000"));
    }

    private Check check(String name, String path) {
        return check(name, path, c -> c.get().uri(path));
    }

    private Check check(String name, String path,
                        java.util.function.Function<org.springframework.web.client.RestClient,
                                org.springframework.web.client.RestClient.RequestHeadersSpec<?>> request) {
        try {
            Rest1Response response = client.call(request);
            return new Check(name, path, true, "RT/" + response.rtVersion() + " " + response.status() + " "
                    + response.statusMessage());
        } catch (RtAccessException e) {
            return new Check(name, path, false, summary(e.getMessage()));
        }
    }

    /** El 404 de RT 6 trae su página web entera: se resume. */
    static String summary(String message) {
        if (message == null) {
            return "Error sin detalle";
        }
        if (message.contains("Page not found")) {
            return "404 Page not found: RT no tiene esta ruta (componente no desplegado o caché de Mason)";
        }
        String oneLine = message.replaceAll("\\s+", " ").strip();
        return oneLine.length() <= MAX_DETAIL ? oneLine : oneLine.substring(0, MAX_DETAIL) + "…";
    }
}
