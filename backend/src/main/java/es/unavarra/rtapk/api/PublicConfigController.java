package es.unavarra.rtapk.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración pública que el frontend lee al arrancar para saber si debe pedir login y a qué
 * Keycloak (realm y client). No contiene secretos: es lo mismo que verá cualquiera en el navegador.
 */
@RestController
public class PublicConfigController {

    public record PublicConfig(boolean securityEnabled, String issuer, String clientId) {
    }

    private final PublicConfig config;

    public PublicConfigController(@Value("${app.security.enabled:true}") boolean securityEnabled,
                                  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuer,
                                  @Value("${app.security.client-id:rt-web}") String clientId) {
        this.config = securityEnabled
                ? new PublicConfig(true, issuer, clientId)
                : new PublicConfig(false, null, null);
    }

    @GetMapping("/api/config")
    public PublicConfig config() {
        return config;
    }
}
