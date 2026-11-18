package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketRef;
import es.unavarra.rtapk.domain.TicketReciente;
import es.unavarra.rtapk.domain.TicketsRecientes;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Tickets activos de la cola configurada actualizados recientemente, con solicitante, departamento y el
 * último mensaje. Por cada ticket se hacen tres consultas a RT (ticket, lista de adjuntos y contenido del
 * último text/plain), en paralelo con un límite para no saturar RT.
 */
@Service
public class RecentTicketService {

    private static final Logger log = LoggerFactory.getLogger(RecentTicketService.class);
    private static final int MAX_PARALLEL_TICKETS = 6;

    private final RtGateway rt;
    private final String queue;
    private final String updatedSince;
    private final Semaphore permits = new Semaphore(MAX_PARALLEL_TICKETS);

    public RecentTicketService(RtGateway rt,
                               @Value("${app.tickets.queue}") String queue,
                               @Value("${app.tickets.updated-since}") String updatedSince) {
        this.rt = rt;
        this.queue = queue;
        this.updatedSince = updatedSince;
    }

    /** Vista de cola: lo ve todo, así que solo para agentes. */
    public TicketsRecientes recent(CurrentUser user) {
        if (!user.agent()) {
            throw new AccessDeniedException("Solo los agentes pueden ver los tickets de la cola");
        }
        List<TicketRef> refs = rt.findRecentActive(queue, updatedSince);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<TicketReciente>> futures = refs.stream()
                    .map(ref -> CompletableFuture.supplyAsync(() -> limited(ref), executor))
                    .toList();
            List<TicketReciente> tickets = futures.stream().map(CompletableFuture::join).toList();
            return new TicketsRecientes(queue, updatedSince, tickets);
        }
    }

    private TicketReciente limited(TicketRef ref) {
        try {
            permits.acquire();
            try {
                return load(ref);
            } finally {
                permits.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return partial(ref);
        }
    }

    private TicketReciente load(TicketRef ref) {
        try {
            Ticket ticket = rt.getTicket(ref.id());
            return new TicketReciente(
                    ref.id(),
                    ticket.subject() != null ? ticket.subject() : ref.subject(),
                    ticket.status(),
                    ticket.owner(),
                    String.join(", ", ticket.requestors()),
                    blankToNull(ticket.customFields().get("NDepartamento")),
                    ticket.lastUpdated(),
                    lastPlainTextMessage(ref.id()));
        } catch (RuntimeException e) {
            // Un ticket que falla no debe tirar la lista entera
            log.warn("No se han podido cargar los datos del ticket {}", ref.id(), e);
            return partial(ref);
        }
    }

    /** Contenido del adjunto text/plain con mayor id (el mensaje más reciente). */
    String lastPlainTextMessage(long ticketId) {
        return rt.getAttachments(ticketId).stream()
                .filter(AttachmentRef::isTextPlain)
                .max(Comparator.comparingLong(AttachmentRef::id))
                .map(a -> rt.getAttachmentContent(a.id()))
                .orElse(null);
    }

    private static TicketReciente partial(TicketRef ref) {
        return new TicketReciente(ref.id(), ref.subject(), null, null, null, null, null, null);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
