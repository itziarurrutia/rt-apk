package es.unavarra.rtapk.security;

import java.util.Set;

/**
 * Usuario de la petición actual (de Keycloak; RT no lo conoce).
 *
 * @param username nombre de usuario de Keycloak ({@code preferred_username})
 * @param email    correo, si el usuario lo tiene en Keycloak; puede ser {@code null}
 * @param agent    si tiene el rol de agente puede ver todos los tickets; si no, solo aquellos en los que es solicitante
 * @param roles    roles de realm de Keycloak (sin el prefijo ROLE_)
 */
public record CurrentUser(String username, String email, String name, boolean agent, Set<String> roles) {

    public CurrentUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public CurrentUser(String username, String email, String name, boolean agent) {
        this(username, email, name, agent, Set.of());
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}
