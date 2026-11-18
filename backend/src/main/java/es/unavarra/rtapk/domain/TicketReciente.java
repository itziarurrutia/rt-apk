package es.unavarra.rtapk.domain;

import java.time.OffsetDateTime;

/**
 * Ticket de la vista de tickets recientes de la cola.
 *
 * @param solicitante  Requestors del ticket
 * @param departamento campo personalizado NDepartamento
 * @param contenido    texto del adjunto text/plain más reciente (el último mensaje); null si no hay
 */
public record TicketReciente(
        long id,
        String asunto,
        String estado,
        String propietario,
        String solicitante,
        String departamento,
        OffsetDateTime ultimaActualizacion,
        String contenido) {
}
