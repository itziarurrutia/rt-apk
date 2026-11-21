package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketRef;
import es.unavarra.rtapk.domain.TicketReciente;
import es.unavarra.rtapk.domain.TicketsRecientes;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.rt.TicketSqlBuilder;
import es.unavarra.rtapk.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tickets activos recientes, con solicitante, departamento y el último mensaje. La consulta depende del
 * rol de Keycloak del usuario ({@link TicketQueriesProperties}); sin rol con consulta propia, los agentes
 * ven la cola por defecto. Por cada ticket se hacen tres consultas a RT (ticket, lista de adjuntos y contenido del
 * último text/plain), en paralelo con un límite para no saturar RT.
 */
@Service
public class RecentTicketService {

    private static final Logger log = LoggerFactory.getLogger(RecentTicketService.class);
    private static final int MAX_PARALLEL_TICKETS = 6;
    /** Tickets de la vista "Tickets" (los del usuario como propietario). */
    static final int MINE_LIMIT = 5;
    /** Resultados de la búsqueda en "Tickets" (cada ticket son tres consultas a RT). */
    public static final int SEARCH_LIMIT = 30;
    /** Marcadores de las consultas por rol. */
    static final String USERNAME = "{usuario}";
    static final String EMAIL = "{email}";

    private final RtGateway rt;
    private final String queue;
    private final String updatedSince;
    private final Map<String, String> queriesByRole;
    private final TicketQueriesProperties queries;
    private final Semaphore permits = new Semaphore(MAX_PARALLEL_TICKETS);

    @Autowired
    public RecentTicketService(RtGateway rt,
                               @Value("${app.tickets.queue}") String queue,
                               @Value("${app.tickets.updated-since}") String updatedSince,
                               TicketQueriesProperties queries) {
        this.rt = rt;
        this.queue = queue;
        this.updatedSince = updatedSince;
        this.queriesByRole = queries.queriesByRole();
        this.queries = queries;
    }

    public RecentTicketService(RtGateway rt, String queue, String updatedSince) {
        this(rt, queue, updatedSince, new TicketQueriesProperties(Map.of()));
    }

    public TicketsRecientes recent(CurrentUser user) {
        List<String> profiles = profilesOf(user);
        if (!profiles.isEmpty()) {
            Map<String, String> descriptions = profiles.stream()
                    .collect(Collectors.toMap(p -> p, queries::descriptionOf, (a, b) -> a, java.util.LinkedHashMap::new));
            return new TicketsRecientes(null, null, load(rt.findByQuery(profileQuery(user, profiles))), profiles,
                    descriptions);
        }
        if (!user.agent()) {
            // La vista de cola lo ve todo, así que solo para agentes
            throw new AccessDeniedException("No tienes un perfil con consulta de tickets");
        }
        return new TicketsRecientes(queue, updatedSince, load(rt.findRecentActive(queue, updatedSince)), List.of());
    }

    /**
     * Los {@value #MINE_LIMIT} tickets activos más recientes (por fecha de creación) de los que el usuario es
     * propietario en RT ({@code Owner} = su nombre de usuario de Keycloak), con su último mensaje.
     */
    public List<TicketReciente> mine(CurrentUser user) {
        List<TicketRef> refs = rt.findByQuery(TicketSqlBuilder.activeOwnedBy(user.username()), "-Created");
        return load(refs.stream().limit(MINE_LIMIT).toList());
    }

    /**
     * Búsqueda entre todos los tickets activos del usuario como propietario: por asunto o, si el texto lleva
     * "@", también por solicitante. Los más recientes primero, como mucho {@value #SEARCH_LIMIT}.
     */
    public List<TicketReciente> searchMine(CurrentUser user, String text) {
        String sql = TicketSqlBuilder.activeOwnedByMatching(user.username(), text.strip());
        return load(rt.findByQuery(sql, "-Created").stream().limit(SEARCH_LIMIT).toList());
    }

    /**
     * true si el ticket sale en la consulta de alguno de los perfiles del usuario: así puede abrir los
     * tickets de su lista aunque no sea su solicitante.
     */
    public boolean profileIncludes(CurrentUser user, long ticketId) {
        List<String> profiles = profilesOf(user);
        if (profiles.isEmpty()) {
            return false;
        }
        String sql = "(" + profileQuery(user, profiles) + ") AND id = " + ticketId;
        return rt.findByQuery(sql).stream().anyMatch(ref -> ref.id() == ticketId);
    }

    /** Consulta de los perfiles: la del único perfil, o varias unidas con OR. */
    String profileQuery(CurrentUser user, List<String> profiles) {
        return profiles.size() == 1
                ? queryFor(profiles.getFirst(), user)
                : profiles.stream().map(p -> "(" + queryFor(p, user) + ")").collect(Collectors.joining(" OR "));
    }

    /**
     * Consulta del rol con los datos del usuario autenticado: {@code {usuario}} (nombre de usuario de
     * Keycloak) y {@code {email}}. Los valores se escapan, así que van dentro de comillas:
     * {@code Owner = '{usuario}'}.
     */
    String queryFor(String role, CurrentUser user) {
        String query = queriesByRole.get(role);
        if (query.contains(EMAIL) && !user.hasEmail()) {
            throw new AccessDeniedException("La consulta del perfil " + role + " necesita el correo del usuario");
        }
        return query
                .replace(USERNAME, TicketSqlBuilder.escape(user.username()))
                .replace(EMAIL, user.hasEmail() ? TicketSqlBuilder.escape(user.email()) : "");
    }

    /** Roles del usuario con consulta propia (sin distinguir mayúsculas), en orden alfabético. */
    List<String> profilesOf(CurrentUser user) {
        Set<String> roles = user.roles().stream().map(r -> r.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        return queriesByRole.keySet().stream()
                .filter(role -> roles.contains(role.toLowerCase(Locale.ROOT)))
                .filter(role -> !queriesByRole.get(role).isBlank())
                .toList();
    }

    private List<TicketReciente> load(List<TicketRef> refs) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<TicketReciente>> futures = refs.stream()
                    .map(ref -> CompletableFuture.supplyAsync(() -> limited(ref), executor))
                    .toList();
            return futures.stream().map(CompletableFuture::join).toList();
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

    /**
     * Contenido del mensaje más reciente: el adjunto text/plain o text/html con mayor id. Un correo llega
     * como multipart/alternative con text/plain y, justo después (id + 1), text/html: en ese caso se usa
     * el text/plain. RT 6 guarda solo en text/html los mensajes escritos desde su web; si el contenido
     * llega en HTML, se pasa a texto.
     */
    String lastPlainTextMessage(long ticketId) {
        List<AttachmentRef> attachments = rt.getAttachments(ticketId);
        AttachmentRef newest = attachments.stream()
                .filter(AttachmentRef::isMessageBody)
                .max(Comparator.comparingLong(AttachmentRef::id))
                .orElse(null);
        AttachmentRef last = newest == null || !newest.isHtml() ? newest : attachments.stream()
                .filter(a -> a.isTextPlain() && a.id() == newest.id() - 1)
                .findFirst()
                .orElse(newest);
        String content = last == null ? null : rt.getAttachmentContent(last.id());
        log.debug("Ticket {}: adjuntos {}, último mensaje {}, contenido de {} caracteres", ticketId, attachments,
                last == null ? "ninguno" : last.id(), content == null ? 0 : content.length());
        return content != null && looksLikeHtml(content) ? htmlToText(content) : content;
    }

    private static final Pattern HTML_TAG = Pattern.compile("(?is)<\\s*/?\\s*[a-z][^>]*>");

    static boolean looksLikeHtml(String content) {
        return HTML_TAG.matcher(content).find();
    }

    /** Texto legible de un mensaje HTML sencillo: saltos de línea en br/p/div/li y sin etiquetas. */
    static String htmlToText(String html) {
        String text = html
                .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", "")
                .replaceAll("(?i)<\\s*br\\s*/?>", "\n")
                .replaceAll("(?i)</\\s*(p|div|li|tr|h[1-6])\\s*>", "\n");
        text = HTML_TAG.matcher(text).replaceAll("")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
        return text.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").strip();
    }

    private static TicketReciente partial(TicketRef ref) {
        return new TicketReciente(ref.id(), ref.subject(), null, null, null, null, null, null);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
