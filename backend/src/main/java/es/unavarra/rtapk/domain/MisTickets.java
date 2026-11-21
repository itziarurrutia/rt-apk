package es.unavarra.rtapk.domain;

import java.util.List;

/**
 * Respuesta de la vista "Mis tickets".
 *
 * @param tickets tickets activos del usuario como propietario, los más recientes primero
 * @param limite  máximo de tickets de la respuesta (sin búsqueda, {@code app.tickets.mine-limit}; con
 *                búsqueda, el límite de resultados): si {@code tickets} llega a él, puede haber más
 */
public record MisTickets(List<TicketReciente> tickets, int limite) {
}
