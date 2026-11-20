package es.unavarra.rtapk.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.TreeMap;

/**
 * Consulta TicketSQL de la vista de tickets según el rol de Keycloak del usuario
 * ({@code app.tickets.queries-by-role.<rol>}). Si el usuario tiene varios, se unen con OR.
 * {@code {usuario}} y {@code {email}} se sustituyen por los del usuario autenticado, p. ej.
 * {@code Owner = '{usuario}'}.
 * Los usuarios sin ninguno de estos roles usan la consulta por defecto (cola y horas).
 */
@ConfigurationProperties("app.tickets")
public record TicketQueriesProperties(Map<String, String> queriesByRole) {

    public TicketQueriesProperties {
        // Ordenado por rol: la consulta combinada sale siempre igual
        queriesByRole = queriesByRole == null ? Map.of() : new TreeMap<>(queriesByRole);
    }
}
