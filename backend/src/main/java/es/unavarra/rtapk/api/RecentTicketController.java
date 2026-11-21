package es.unavarra.rtapk.api;

import es.unavarra.rtapk.domain.TicketReciente;
import es.unavarra.rtapk.domain.TicketsRecientes;
import es.unavarra.rtapk.security.CurrentUserResolver;
import es.unavarra.rtapk.service.RecentTicketService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Size;

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

    /**
     * Tickets activos de los que el usuario es propietario, con su último mensaje: sin {@code q}, los 5 más
     * recientes; con {@code q}, los que lo contienen en el asunto (o en el solicitante si lleva "@").
     */
    @GetMapping("/api/tickets/mios")
    public List<TicketReciente> mine(Authentication auth,
                                     @RequestParam(required = false) @Size(max = 200) String q) {
        return q == null || q.isBlank()
                ? recentTickets.mine(users.resolve(auth))
                : recentTickets.searchMine(users.resolve(auth), q);
    }
}
