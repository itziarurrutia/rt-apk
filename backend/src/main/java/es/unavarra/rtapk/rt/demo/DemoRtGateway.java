package es.unavarra.rtapk.rt.demo;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketNotFoundException;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.domain.TicketRef;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.rt.RtProperties;
import es.unavarra.rtapk.rt.rest1.Rest1TicketMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tickets de ejemplo en memoria ({@code rt.api-version=demo}), para trabajar sin RT. Las respuestas
 * añadidas se guardan en memoria y se pierden al reiniciar.
 */
@Component
@ConditionalOnProperty(name = "rt.api-version", havingValue = "demo")
public class DemoRtGateway implements RtGateway {

    private static final Logger log = LoggerFactory.getLogger(DemoRtGateway.class);
    private static final Set<String> ACTIVE = Set.of("new", "open", "stalled");

    private final List<Ticket> tickets;
    private final Map<Long, List<TicketHistoryEntry>> history = new ConcurrentHashMap<>();
    private final Map<Long, List<AttachmentRef>> attachments = new ConcurrentHashMap<>();
    private final Map<Long, String> attachmentContents = new ConcurrentHashMap<>();
    private final AtomicLong transactionIds = new AtomicLong(900_000);
    private final AtomicLong attachmentIds = new AtomicLong(17_900_000);

    public DemoRtGateway(RtProperties properties) {
        Rest1TicketMapper mapper = new Rest1TicketMapper(properties.timeZone());
        this.tickets = new CopyOnWriteArrayList<>(DemoData.load("tickets.txt").stream().map(mapper::toTicket).toList());
        for (Ticket t : tickets) {
            String requestor = t.requestors().getFirst();
            String message = "Hola,\n\n" + t.subject() + ".\nLo he probado esta mañana y sigue igual.\n\n-- \n"
                    + requestor + "\nUniversidad Pública de Navarra";
            history.put(t.id(), new CopyOnWriteArrayList<>(List.of(
                    new TicketHistoryEntry(transactionIds.incrementAndGet(), "Create", requestor,
                            t.created(), "Ticket created", message),
                    new TicketHistoryEntry(transactionIds.incrementAndGet(), "Comment", t.owner(),
                            t.created().plusHours(1), "Comments added", "Nota interna: revisar."))));
            attachments.put(t.id(), new CopyOnWriteArrayList<>());
            addMessage(t.id(), message);
        }
    }

    @Override
    public List<Ticket> findTickets(TicketQuery query) {
        return tickets.stream()
                .filter(t -> query.requestor() == null || t.hasRequestor(query.requestor()))
                .filter(t -> query.statuses().isEmpty() || query.statuses().contains(t.status()))
                .filter(t -> query.queue() == null || query.queue().isBlank() || query.queue().equalsIgnoreCase(t.queue()))
                .filter(t -> query.subjectContains() == null
                        || t.subject().toLowerCase().contains(query.subjectContains().toLowerCase()))
                .toList();
    }

    /** En demo no se filtra por fecha: devuelve todos los activos de la cola. */
    @Override
    public List<TicketRef> findRecentActive(String queue, String updatedSince) {
        return tickets.stream()
                .filter(t -> queue.equalsIgnoreCase(t.queue()) && ACTIVE.contains(t.status()))
                .map(t -> new TicketRef(t.id(), t.subject()))
                .toList();
    }

    /** En demo no se interpreta TicketSQL: devuelve todos los tickets activos. */
    @Override
    public List<TicketRef> findByQuery(String ticketSql) {
        return tickets.stream()
                .filter(t -> ACTIVE.contains(t.status()))
                .map(t -> new TicketRef(t.id(), t.subject()))
                .toList();
    }

    @Override
    public Ticket getTicket(long id) {
        return tickets.stream().filter(t -> t.id() == id).findFirst()
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Override
    public List<TicketHistoryEntry> getHistory(long ticketId) {
        getTicket(ticketId);
        return List.copyOf(history.get(ticketId));
    }

    @Override
    public List<AttachmentRef> getAttachments(long ticketId) {
        getTicket(ticketId);
        return List.copyOf(attachments.get(ticketId));
    }

    @Override
    public String getAttachmentContent(long attachmentId) {
        return attachmentContents.get(attachmentId);
    }

    @Override
    public void addReply(long ticketId, String text) {
        getTicket(ticketId);
        history.get(ticketId).add(new TicketHistoryEntry(transactionIds.incrementAndGet(), "Correspond",
                "svc-rt-apk", OffsetDateTime.now(), "Correspondence added", text));
        addMessage(ticketId, text);
    }

    /** Comentario interno en el historial; en demo los assets no son tickets, así que en ellos solo va al log. */
    @Override
    public void addComment(long ticketId, String subject, String text) {
        log.info("Demo: comentario en el ticket {}: {}", ticketId, subject);
        List<TicketHistoryEntry> entries = history.get(ticketId);
        if (entries != null) {
            entries.add(new TicketHistoryEntry(transactionIds.incrementAndGet(), "Comment", "svc-rt-apk",
                    OffsetDateTime.now(), "Comments added", text));
        }
    }

    @Override
    public void updateTicket(long ticketId, String status, Map<String, String> customFields) {
        Ticket t = getTicket(ticketId);
        Map<String, String> fields = new java.util.LinkedHashMap<>(t.customFields());
        fields.putAll(customFields);
        tickets.set(tickets.indexOf(t), new Ticket(t.id(), t.subject(), t.queue(), status != null ? status : t.status(),
                t.owner(), t.requestors(), t.priority(), t.created(), OffsetDateTime.now(), Map.copyOf(fields)));
        log.info("Demo: ticket {} actualizado: estado {}, campos {}", ticketId, status, customFields);
    }

    /** Como hace RT con un correo: multipart/alternative con una parte text/plain y otra text/html. */
    private void addMessage(long ticketId, String text) {
        List<AttachmentRef> list = attachments.get(ticketId);
        list.add(new AttachmentRef(attachmentIds.incrementAndGet(), "multipart/alternative"));
        long plain = attachmentIds.incrementAndGet();
        list.add(new AttachmentRef(plain, "text/plain"));
        list.add(new AttachmentRef(attachmentIds.incrementAndGet(), "text/html"));
        attachmentContents.put(plain, "Ticket URL: https://rt.example.org/Ticket/Display.html?id=" + ticketId + "\n\n" + text);
    }
}
