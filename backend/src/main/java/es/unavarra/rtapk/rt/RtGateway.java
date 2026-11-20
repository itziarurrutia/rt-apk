package es.unavarra.rtapk.rt;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.domain.TicketRef;

import java.util.List;

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

    /** @throws es.unavarra.rtapk.domain.TicketNotFoundException si el ticket no existe */
    Ticket getTicket(long id);

    List<TicketHistoryEntry> getHistory(long ticketId);

    List<AttachmentRef> getAttachments(long ticketId);

    /** Contenido de un adjunto; null si no tiene. */
    String getAttachmentContent(long attachmentId);

    /** Añade una respuesta (correspondencia) visible para los solicitantes. */
    void addReply(long ticketId, String text);
}
