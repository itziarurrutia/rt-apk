package es.unavarra.rtapk.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    /** Usuario fijo cuando la seguridad está desactivada (perfil demo): agente, lo ve todo. */
    static final CurrentUser DEMO_USER = new CurrentUser("demo@example.org", "Usuario demo", true);

    private final boolean securityEnabled;
    private final String agentAuthority;

    public CurrentUserResolver(@Value("${app.security.enabled:true}") boolean securityEnabled,
                               @Value("${app.security.agent-role}") String agentRole) {
        this.securityEnabled = securityEnabled;
        this.agentAuthority = "ROLE_" + agentRole;
    }

    public CurrentUser resolve(Authentication authentication) {
        if (!securityEnabled) {
            return DEMO_USER;
        }
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Se requiere un token JWT");
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            // Sin email no se puede filtrar por solicitante en RT
            throw new AccessDeniedException("El token no contiene el claim email");
        }
        boolean agent = authentication.getAuthorities().stream()
                .anyMatch(a -> agentAuthority.equals(a.getAuthority()));
        String name = jwt.hasClaim("name") ? jwt.getClaimAsString("name") : email;
        return new CurrentUser(email, name, agent);
    }
}
