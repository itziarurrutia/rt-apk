package es.unavarra.rtapk.rt;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Añade las credenciales de RT a cada petición como parámetros {@code user} y {@code pass}, igual que
 * {@code <url>/search/ticket?query=...&user=...&pass=...}. Así no hace falta login ni cookie de sesión.
 *
 * <p>Como la contraseña viaja en la URL, nunca se debe registrar una URL de RT sin pasarla por
 * {@link #redact(String)}.
 */
public class RtCredentialsInterceptor implements ClientHttpRequestInterceptor {

    private static final Pattern PASS_PARAM = Pattern.compile("([?&]pass=)[^&\\s\"']*");

    private final String username;
    private final Supplier<String> password;

    /** @param password se lee en cada petición (permite cambiar el secret sin reiniciar) */
    public RtCredentialsInterceptor(String username, Supplier<String> password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        URI withCredentials = UriComponentsBuilder.fromUri(request.getURI())
                .queryParam("user", encode(username))
                .queryParam("pass", encode(password.get()))
                .build(true)
                .toUri();
        return execution.execute(new HttpRequestWrapper(request) {
            @Override
            public URI getURI() {
                return withCredentials;
            }
        }, body);
    }

    /** Sustituye el valor de {@code pass=} por {@code ***} (para mensajes de error y logs). */
    public static String redact(String text) {
        return text == null ? null : PASS_PARAM.matcher(text).replaceAll("$1***");
    }

    private static String encode(String value) {
        return UriUtils.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
