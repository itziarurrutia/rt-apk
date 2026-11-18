package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.function.Function;

/**
 * Cliente HTTP de RT REST 1.0 compartido por los adaptadores de tickets y de assets.
 *
 * <p>Se autentica con usuario/contraseña haciendo login una vez y reutilizando la cookie de sesión de
 * RT. Si RT contesta {@code 401 Credentials required} (sesión caducada) repite el login y la petición.
 */
@Component
@ConditionalOnExpression("'${rt.api-version:rest1}' != 'demo'")
public class Rest1Client {

    private static final Logger log = LoggerFactory.getLogger(Rest1Client.class);

    static final String BASE = "/REST/1.0";

    private final RestClient restClient;
    private final RtProperties properties;
    private final Rest1ResponseParser parser = new Rest1ResponseParser();

    private volatile String sessionCookie;

    public Rest1Client(@Qualifier("rtRestClient") RestClient restClient, RtProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /** Ejecuta la petición (con login si hace falta) y devuelve la respuesta parseada con estado 200. */
    public Rest1Response call(Function<RestClient, RestClient.RequestHeadersSpec<?>> request) {
        if (sessionCookie == null) {
            login();
        }
        Rest1Response response = execute(request);
        if (response.status() == 401) {
            log.info("Sesión de RT caducada, se repite el login");
            login();
            response = execute(request);
        }
        if (!response.isOk()) {
            throw new RtAccessException("RT respondió " + response.status() + " " + response.statusMessage());
        }
        return response;
    }

    private Rest1Response execute(Function<RestClient, RestClient.RequestHeadersSpec<?>> request) {
        try {
            byte[] body = request.apply(restClient)
                    .header(HttpHeaders.COOKIE, sessionCookie)
                    .retrieve()
                    .body(byte[].class);
            return parser.parse(decode(body));
        } catch (RestClientException e) {
            throw new RtAccessException("Error de comunicación con RT", e);
        }
    }

    private synchronized void login() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("user", properties.username());
        form.add("pass", properties.resolvePassword());
        ResponseEntity<byte[]> entity;
        try {
            entity = restClient.post().uri(BASE + "/")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(byte[].class);
        } catch (RestClientException e) {
            throw new RtAccessException("Error de comunicación con RT durante el login", e);
        }
        Rest1Response response = parser.parse(decode(entity.getBody()));
        if (!response.isOk()) {
            throw new RtAccessException("Login en RT fallido: " + response.status() + " " + response.statusMessage());
        }
        sessionCookie = entity.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
                .filter(c -> c.startsWith("RT_SID"))
                .map(c -> c.split(";", 2)[0])
                .findFirst()
                .orElseThrow(() -> new RtAccessException("RT no devolvió cookie de sesión en el login"));
    }

    private String decode(byte[] body) {
        return body == null ? "" : new String(body, properties.charset());
    }
}
