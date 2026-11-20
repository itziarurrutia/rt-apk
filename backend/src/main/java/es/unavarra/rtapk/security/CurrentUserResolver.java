package es.unavarra.rtapk.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CurrentUserResolver {

    /** Usuario fijo cuando la seguridad está desactivada (perfil demo): agente, lo ve todo. */
    static final CurrentUser DEMO_USER = new CurrentUser("demo", "demo@example.org", "Usuario demo", true);

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
        // Los usuarios entran con nombre de usuario; el correo es opcional
        String email = blankToNull(jwt.getClaimAsString("email"));
        String username = firstNonNull(blankToNull(jwt.getClaimAsString("preferred_username")), email);
        if (username == null) {
            throw new AccessDeniedException("El token no identifica al usuario (preferred_username)");
        }
        boolean agent = authentication.getAuthorities().stream()
                .anyMatch(a -> agentAuthority.equals(a.getAuthority()));
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .collect(Collectors.toSet());
        String name = firstNonNull(blankToNull(jwt.getClaimAsString("name")), username);
        return new CurrentUser(username, email, name, agent, roles);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
