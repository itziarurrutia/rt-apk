package es.unavarra.rtapk.api;

import es.unavarra.rtapk.domain.TicketReciente;
import es.unavarra.rtapk.domain.TicketsRecientes;
import es.unavarra.rtapk.security.CurrentUserResolver;
import es.unavarra.rtapk.service.RecentTicketService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecentTicketController {

    private final RecentTicketService recentTickets;
    private final CurrentUserResolver users;

    public RecentTicketController(RecentTicketService recentTickets, CurrentUserResolver users) {
        this.recentTickets = recentTickets;
        this.users = users;
    }

    /** Tickets activos de la cola configurada actualizados recientemente, con su último mensaje. */
    @GetMapping("/api/tickets/recientes")
    public TicketsRecientes recent(Authentication auth) {
        return recentTickets.recent(users.resolve(auth));
    }

    /** Los 5 tickets activos más recientes de los que el usuario es propietario, con su último mensaje. */
    @GetMapping("/api/tickets/mios")
    public List<TicketReciente> mine(Authentication auth) {
        return recentTickets.mine(users.resolve(auth));
    }
}
