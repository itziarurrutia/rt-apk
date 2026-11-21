package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketNotFoundException;
import es.unavarra.rtapk.domain.TicketPartiallyUpdatedException;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reglas de acceso sobre RT. El backend entra en RT con una cuenta de servicio que lo ve todo, así que
 * aquí se restringe cada operación al usuario del token: un usuario normal solo ve los tickets en los
 * que es solicitante y no ve los comentarios internos.
 */
@Service
public class TicketService {

    static final String RESOLVED = "resolved";

    private final RtGateway rt;
    private final RecentTicketService profiles;

    public TicketService(RtGateway rt, RecentTicketService profiles) {
        this.rt = rt;
        this.profiles = profiles;
    }

    public List<Ticket> search(CurrentUser user, TicketQuery query) {
        if (!user.agent() && !user.hasEmail()) {
            // Sin correo no se puede saber de qué tickets es solicitante
            return List.of();
        }
        TicketQuery scoped = user.agent() ? query : query.withRequestor(user.email());
        return rt.findTickets(scoped);
    }

    public Ticket get(CurrentUser user, long id) {
        Ticket ticket = rt.getTicket(id);
        boolean requestor = user.hasEmail() && ticket.hasRequestor(user.email());
        // Sin rt-agent: solo sus tickets como solicitante o propietario, o los que devuelve la consulta de su perfil
        if (!user.agent() && !requestor && !isOwner(user, ticket) && !profiles.profileIncludes(user, id)) {
            // 404 y no 403 para no revelar qué tickets existen
            throw new TicketNotFoundException(id);
        }
        return ticket;
    }

    /**
     * Comentario interno (no lo ven los solicitantes) y, si {@code resolve}, el ticket pasa a resuelto. Al resolver
     * un ticket de apoyo.dpto, si {@code inventory} no es null y cambia, se actualiza también su campo Inventario
     * (en la misma operación que el estado). Solo el propietario del ticket o un agente. En RT el comentario
     * figura como de la cuenta de servicio: se firma con el usuario real.
     *
     * @throws TicketPartiallyUpdatedException si el comentario se ha guardado pero no se ha podido resolver
     */
    public void comment(CurrentUser user, long id, String text, boolean resolve, String inventory) {
        Ticket ticket = get(user, id);
        if (!user.agent() && !isOwner(user, ticket)) {
            throw new AccessDeniedException("Solo el propietario del ticket puede comentarlo");
        }
        // Asunto: "Resuelto por <usuario> via rt-mobile" si se resuelve; si no, "Comentario de <usuario> via rt-mobile"
        String subject = (resolve ? "Resuelto por " : "Comentario de ") + user.username() + " via rt-mobile";
        rt.addComment(id, subject,
                text.strip() + "\n\n-- \nEnviado por " + user.name() + " (" + user.username() + ") desde rt-mobile");
        if (resolve) {
            Map<String, String> fields = new LinkedHashMap<>();
            String newInventory = inventory == null ? null : inventory.strip();
            if (ticket.inDepartmentQueue() && newInventory != null
                    && !newInventory.equals(ticket.inventory() == null ? "" : ticket.inventory())) {
                fields.put(Ticket.INVENTORY_FIELD, newInventory);
            }
            try {
                rt.updateTicket(id, RESOLVED, fields);
            } catch (RuntimeException e) {
                throw new TicketPartiallyUpdatedException(
                        "El comentario se ha guardado, pero no se ha podido resolver el ticket " + id, e);
            }
        }
    }

    private static boolean isOwner(CurrentUser user, Ticket ticket) {
        return ticket.owner() != null && ticket.owner().equalsIgnoreCase(user.username());
    }

    public List<TicketHistoryEntry> history(CurrentUser user, long id) {
        get(user, id);
        List<TicketHistoryEntry> history = rt.getHistory(id);
        return user.agent() ? history : history.stream().filter(TicketHistoryEntry::isPublicMessage).toList();
    }

    public void reply(CurrentUser user, long id, String text) {
        get(user, id);
        // En RT la respuesta figura como enviada por la cuenta de servicio: se deja constancia del autor real
        rt.addReply(id, text.strip() + "\n\n-- \nEnviado por " + user.name() + " (" + user.username() + ") desde rt-mobile");
    }
}
