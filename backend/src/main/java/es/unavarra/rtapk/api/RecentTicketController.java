package es.unavarra.rtapk.api;

import es.unavarra.rtapk.domain.MisTickets;
import es.unavarra.rtapk.domain.TicketsRecientes;
import es.unavarra.rtapk.security.CurrentUserResolver;
import es.unavarra.rtapk.service.RecentTicketService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Size;

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

    /**
     * Tickets activos de los que el usuario es propietario, con su último mensaje: sin {@code q}, los más
     * recientes ({@code app.tickets.mine-limit}); con {@code q}, los que lo contienen en el asunto (o también
     * en el solicitante si lleva "@").
     */
    @GetMapping("/api/tickets/mios")
    public MisTickets mine(Authentication auth,
                           @RequestParam(required = false) @Size(max = 200) String q) {
        return q == null || q.isBlank()
                ? recentTickets.mine(users.resolve(auth))
                : recentTickets.searchMine(users.resolve(auth), q);
    }
}
