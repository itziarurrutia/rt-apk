package es.unavarra.rtapk.rt;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.domain.TicketRef;

import java.util.List;
import java.util.Map;

/**
 * Puerto de acceso a Request Tracker. Hay una implementación por versión de la API de RT
 * ({@code rt.api-version}); el resto de la aplicación solo conoce esta interfaz.
 */
public interface RtGateway {

    List<Ticket> findTickets(TicketQuery query);

    /** Tickets activos de la cola creados desde {@code updatedSince} (expresión de RT, p. ej. "5 hours ago"). */
    default List<TicketRef> findRecentActive(String queue, String updatedSince) {
        return findByQuery(TicketSqlBuilder.recentActive(queue, updatedSince));
    }

    /** Tickets que cumplen una consulta TicketSQL (de la configuración, nunca del usuario): id y asunto. */
    List<TicketRef> findByQuery(String ticketSql);

    /**
     * Como {@link #findByQuery(String)} pero ordenados por {@code orderBy} (campo de RT; con "-" delante,
     * descendente, p. ej. "-Created"). Por defecto, sin ordenar.
     */
    default List<TicketRef> findByQuery(String ticketSql, String orderBy) {
        return findByQuery(ticketSql);
    }

    /** @throws es.unavarra.rtapk.domain.TicketNotFoundException si el ticket no existe */
    Ticket getTicket(long id);

    List<TicketHistoryEntry> getHistory(long ticketId);

    List<AttachmentRef> getAttachments(long ticketId);

    /** Contenido de un adjunto; null si no tiene. */
    String getAttachmentContent(long attachmentId);

    /** Añade una respuesta (correspondencia) visible para los solicitantes. */
    void addReply(long ticketId, String text);

    /**
     * Añade un comentario interno (Action: comment, no lo ven los solicitantes) con asunto y texto plano.
     * Sirve también para los assets, que en RT son tickets.
     */
    void addComment(long ticketId, String subject, String text);

    /** Cambia el estado del ticket (p. ej. "resolved"). */
    default void setStatus(long ticketId, String status) {
        updateTicket(ticketId, status, Map.of());
    }

    /**
     * Cambia en una sola operación el estado (si no es null) y los campos personalizados indicados
     * (nombre del campo: valor; un valor vacío lo borra).
     */
    void updateTicket(long ticketId, String status, Map<String, String> customFields);
}
