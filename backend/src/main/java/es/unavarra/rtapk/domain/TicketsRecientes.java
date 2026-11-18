package es.unavarra.rtapk.domain;

import java.util.List;

/**
 * Vista de tickets recientes de una cola.
 *
 * @param actualizadosDesde expresión de RT usada en la búsqueda, p. ej. "5 hours ago"
 */
public record TicketsRecientes(String cola, String actualizadosDesde, List<TicketReciente> tickets) {
}
