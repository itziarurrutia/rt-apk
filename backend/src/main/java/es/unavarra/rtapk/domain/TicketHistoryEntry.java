package es.unavarra.rtapk.domain;

import java.time.OffsetDateTime;

/**
 * Transacción del historial de un ticket (creación, respuesta, comentario, cambio de estado...).
 *
 * @param type tipo de transacción de RT: Create, Correspond, Comment, Status, ...
 */
public record TicketHistoryEntry(
        long id,
        String type,
        String creator,
        OffsetDateTime created,
        String description,
        String content) {

    /** Transacciones con mensaje visible para el solicitante. Los comentarios son internos de RT. */
    public boolean isPublicMessage() {
        return "Create".equals(type) || "Correspond".equals(type);
    }
}
