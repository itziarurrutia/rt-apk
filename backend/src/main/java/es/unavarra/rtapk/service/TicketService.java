package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketNotFoundException;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reglas de acceso sobre RT. El backend entra en RT con una cuenta de servicio que lo ve todo, así que
 * aquí se restringe cada operación al usuario del token: un usuario normal solo ve los tickets en los
 * que es solicitante y no ve los comentarios internos.
 */
@Service
public class TicketService {

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
        // Sin rt-agent: solo sus tickets como solicitante o los que devuelve la consulta de su perfil
        if (!user.agent() && !requestor && !profiles.profileIncludes(user, id)) {
            // 404 y no 403 para no revelar qué tickets existen
            throw new TicketNotFoundException(id);
        }
        return ticket;
    }

    public List<TicketHistoryEntry> history(CurrentUser user, long id) {
        get(user, id);
        List<TicketHistoryEntry> history = rt.getHistory(id);
        return user.agent() ? history : history.stream().filter(TicketHistoryEntry::isPublicMessage).toList();
    }

    public void reply(CurrentUser user, long id, String text) {
        get(user, id);
        // En RT la respuesta figura como enviada por la cuenta de servicio: se deja constancia del autor real
        rt.addReply(id, text.strip() + "\n\n-- \nEnviado por " + user.name() + " (" + user.username() + ") desde rt-apk");
    }
}
