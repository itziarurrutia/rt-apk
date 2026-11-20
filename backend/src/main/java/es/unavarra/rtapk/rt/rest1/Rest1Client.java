package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtCredentialsInterceptor;
import es.unavarra.rtapk.rt.RtProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Function;

/**
 * Cliente HTTP de RT REST 1.0 compartido por los adaptadores de tickets y de assets.
 *
 * <p>Las rutas son relativas a {@code rt.url} y las credenciales las añade {@link RtCredentialsInterceptor}
 * a cada petición, así que no hay login ni sesión que mantener.
 */
@Component
@ConditionalOnExpression("'${rt.api-version:rest1}' != 'demo'")
public class Rest1Client {

    private final RestClient restClient;
    private final RtProperties properties;
    private final Rest1ResponseParser parser = new Rest1ResponseParser();

    public Rest1Client(@Qualifier("rtRestClient") RestClient restClient, RtProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /** Ejecuta la petición y devuelve la respuesta parseada con estado 200. */
    public Rest1Response call(Function<RestClient, RestClient.RequestHeadersSpec<?>> request) {
        Rest1Response response = execute(request);
        if (response.status() == 401) {
            throw new RtAccessException("RT rechazó las credenciales (rt.username / rt.password): "
                    + response.statusMessage());
        }
        if (!response.isOk()) {
            // RT explica el motivo en las líneas "# ..." de la respuesta
            String detail = String.join(" | ", response.allComments().toList());
            throw new RtAccessException("RT respondió " + response.status() + " " + response.statusMessage()
                    + (detail.isBlank() ? "" : ": " + RtCredentialsInterceptor.redact(detail)));
        }
        return response;
    }

    private Rest1Response execute(Function<RestClient, RestClient.RequestHeadersSpec<?>> request) {
        byte[] body;
        try {
            body = request.apply(restClient).retrieve().body(byte[].class);
        } catch (RestClientException e) {
            // El mensaje puede incluir la URL con la contraseña: se oculta y no se encadena la excepción original
            Throwable cause = org.springframework.core.NestedExceptionUtils.getMostSpecificCause(e);
            String detail = cause == e ? "" : " [causa: " + cause.getClass().getSimpleName() + ": "
                    + RtCredentialsInterceptor.redact(cause.getMessage()) + "]";
            throw new RtAccessException("Error de comunicación con RT: "
                    + RtCredentialsInterceptor.redact(e.getMessage()) + detail);
        }
        return parser.parse(body == null ? "" : new String(body, properties.charset()));
    }
}
