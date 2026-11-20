package es.unavarra.rtapk.domain;

import java.util.List;

/**
 * Vista de tickets recientes de una cola.
 *
 * @param cola              cola de la consulta por defecto; {@code null} si se ha usado la consulta de un perfil
 * @param actualizadosDesde expresión de RT de la consulta por defecto, p. ej. "5 hours ago"; {@code null} con perfil
 * @param perfiles          roles de Keycloak cuya consulta se ha usado (vacío = consulta por defecto)
 */
public record TicketsRecientes(String cola, String actualizadosDesde, List<TicketReciente> tickets,
                               List<String> perfiles) {

    public TicketsRecientes {
        perfiles = perfiles == null ? List.of() : List.copyOf(perfiles);
    }
}
