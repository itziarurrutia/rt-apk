package es.unavarra.rtapk.rt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@ConditionalOnExpression("'${rt.api-version:rest1}' != 'demo'")
public class RtClientConfig {

    /** Cliente de RT: URL base del REST + usuario y contraseña en cada petición. */
    @Bean
    RestClient rtRestClient(RestClient.Builder builder, RtProperties properties,
                            @Value("${rt.tls.pinned-sha256:}") String pinnedSha256) {
        HttpClient.Builder http = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER);
        if (!pinnedSha256.isBlank()) {
            // Certificado concreto aceptado aunque esté caducado (p. ej. rt5); el resto se valida normal
            http.sslContext(PinnedCertificateTrustManager.sslContext(pinnedSha256));
        }
        HttpClient httpClient = http.build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder
                .baseUrl(stripTrailingSlash(properties.url()))
                .requestFactory(requestFactory)
                .requestInterceptor(new RtCredentialsInterceptor(properties.username(), properties::resolvePassword))
                .build();
    }

    /** Admite "https://rt/REST/1.0" y "https://rt/REST/1.0/" (las rutas ya empiezan por "/"). */
    static String stripTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
