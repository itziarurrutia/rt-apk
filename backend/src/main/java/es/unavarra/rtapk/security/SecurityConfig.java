package es.unavarra.rtapk.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * El backend es un resource server OAuth2: no tiene login propio, solo valida el JWT emitido por
 * Keycloak (firma vía JWKS del realm, emisor, caducidad y audiencia) en cada petición.
 *
 * <p>Con {@code app.security.enabled=false} (perfil demo) la API queda abierta y se usa un usuario fijo.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
    SecurityFilterChain apiSecurity(HttpSecurity http, KeycloakJwtAuthenticationConverter jwtConverter) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/config").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/actuator/**").denyAll()
                        // El resto es el frontend empaquetado (ficheros estáticos y rutas de Angular, ver
                        // SpaWebConfig): público, el login lo hace Angular contra Keycloak
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // API sin cookies (Bearer token), CSRF no aplica
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
    SecurityFilterChain openSecurity(HttpSecurity http) throws Exception {
        log.warn("Seguridad DESACTIVADA (app.security.enabled=false): la API no exige autenticación");
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
