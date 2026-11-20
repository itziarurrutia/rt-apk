package es.unavarra.rtapk.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Map;
import java.util.TreeMap;

/**
 * Consulta TicketSQL de la vista de tickets según el rol de Keycloak del usuario
 * ({@code app.tickets.queries-by-role.<rol>}). Si el usuario tiene varios, se unen con OR.
 * {@code {usuario}} y {@code {email}} se sustituyen por los del usuario autenticado, p. ej.
 * {@code Owner = '{usuario}'}.
 * Los usuarios sin ninguno de estos roles usan la consulta por defecto (cola y horas).
 *
 * <p>{@code app.tickets.descriptions-by-role.<rol>} es el texto que ve el usuario tras "Perfil &lt;rol&gt;";
 * sin él se muestra {@value #DEFAULT_DESCRIPTION}.
 */
@ConfigurationProperties("app.tickets")
public record TicketQueriesProperties(Map<String, String> queriesByRole, Map<String, String> descriptionsByRole) {

    public static final String DEFAULT_DESCRIPTION = "activos";

    @ConstructorBinding
    public TicketQueriesProperties {
        // Ordenado por rol: la consulta combinada sale siempre igual
        queriesByRole = queriesByRole == null ? Map.of() : new TreeMap<>(queriesByRole);
        descriptionsByRole = descriptionsByRole == null ? Map.of() : new TreeMap<>(descriptionsByRole);
    }

    public TicketQueriesProperties(Map<String, String> queriesByRole) {
        this(queriesByRole, null);
    }

    /** Texto del perfil, o {@value #DEFAULT_DESCRIPTION} si no está configurado. */
    public String descriptionOf(String role) {
        String description = descriptionsByRole.get(role);
        return description == null || description.isBlank() ? DEFAULT_DESCRIPTION : description.strip();
    }
}
