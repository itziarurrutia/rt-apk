package es.unavarra.rtapk.security;

/**
 * Usuario de la petición actual.
 *
 * @param agent si tiene el rol de agente puede ver todos los tickets; si no, solo aquellos en los que es solicitante
 */
public record CurrentUser(String email, String name, boolean agent) {
}
