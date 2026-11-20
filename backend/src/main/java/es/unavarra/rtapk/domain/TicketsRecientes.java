package es.unavarra.rtapk.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista de tickets recientes de una cola.
 *
 * @param cola              cola de la consulta por defecto; {@code null} si se ha usado la consulta de un perfil
 * @param actualizadosDesde expresión de RT de la consulta por defecto, p. ej. "5 hours ago"; {@code null} con perfil
 * @param perfiles          roles de Keycloak cuya consulta se ha usado (vacío = consulta por defecto)
 * @param descripciones     texto de cada perfil que se muestra tras "Perfil &lt;rol&gt;"
 */
public record TicketsRecientes(String cola, String actualizadosDesde, List<TicketReciente> tickets,
                               List<String> perfiles, Map<String, String> descripciones) {

    public TicketsRecientes {
        perfiles = perfiles == null ? List.of() : List.copyOf(perfiles);
        // Mismo orden que los perfiles
        descripciones = descripciones == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(descripciones));
    }

    public TicketsRecientes(String cola, String actualizadosDesde, List<TicketReciente> tickets, List<String> perfiles) {
        this(cola, actualizadosDesde, tickets, perfiles, Map.of());
    }
}
