package es.unavarra.rtapk.rt.rest2;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.domain.TicketRef;
import es.unavarra.rtapk.rt.RtGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptador para REST2 ({@code /REST/2.0}, JSON, Basic Auth). Pendiente de implementar en la migración:
 * debe pasar los mismos tests de contrato que {@code Rest1RtGateway}. Se activa con
 * {@code rt.api-version=rest2}; el resto de la aplicación no cambia.
 */
@Component
@ConditionalOnProperty(name = "rt.api-version", havingValue = "rest2")
public class Rest2RtGateway implements RtGateway {

    private static final String NOT_YET = "Adaptador REST2 pendiente de implementar";

    @Override
    public List<Ticket> findTickets(TicketQuery query) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public List<TicketRef> findByQuery(String ticketSql) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public Ticket getTicket(long id) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public List<AttachmentRef> getAttachments(long ticketId) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public String getAttachmentContent(long attachmentId) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public List<TicketHistoryEntry> getHistory(long ticketId) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public void addReply(long ticketId, String text) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public void addComment(long ticketId, String subject, String text) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public long createTicket(String queue, String subject, String requestor, String owner,
                             java.util.Map<String, String> customFields, String text) {
        throw new UnsupportedOperationException(NOT_YET);
    }

    @Override
    public void updateTicket(long ticketId, String status, java.util.Map<String, String> customFields) {
        throw new UnsupportedOperationException(NOT_YET);
    }
}
