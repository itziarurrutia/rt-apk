package es.unavarra.rtapk.security;

/**
 * Usuario de la petición actual (de Keycloak; RT no lo conoce).
 *
 * @param username nombre de usuario de Keycloak ({@code preferred_username})
 * @param email    correo, si el usuario lo tiene en Keycloak; puede ser {@code null}
 * @param agent    si tiene el rol de agente puede ver todos los tickets; si no, solo aquellos en los que es solicitante
 */
public record CurrentUser(String username, String email, String name, boolean agent) {

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }
}
