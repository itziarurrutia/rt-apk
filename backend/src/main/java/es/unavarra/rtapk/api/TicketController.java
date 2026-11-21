package es.unavarra.rtapk.api;

import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.security.CurrentUser;
import es.unavarra.rtapk.security.CurrentUserResolver;
import es.unavarra.rtapk.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService tickets;
    private final CurrentUserResolver users;

    public TicketController(TicketService tickets, CurrentUserResolver users) {
        this.tickets = tickets;
        this.users = users;
    }

    public record ReplyRequest(@NotBlank @Size(max = 10_000) String text) {
    }

    /**
     * Comentario interno; con {@code resolver}, además se resuelve el ticket y, si es de apoyo.dpto y se envía
     * {@code inventario} con otro valor, se cambia su campo Inventario.
     */
    public record CommentRequest(@NotBlank @Size(max = 10_000) String texto, boolean resolver,
                                 @Size(max = 100) String inventario) {
    }

    @GetMapping("/me")
    public CurrentUser me(Authentication auth) {
        return users.resolve(auth);
    }

    @GetMapping("/tickets")
    public List<Ticket> search(Authentication auth,
                               @RequestParam(name = "status", required = false) List<String> statuses,
                               @RequestParam(required = false) String queue,
                               @RequestParam(name = "q", required = false) String subjectContains) {
        return tickets.search(users.resolve(auth), new TicketQuery(null, statuses, queue, subjectContains));
    }

    @GetMapping("/tickets/{id}")
    public Ticket get(Authentication auth, @PathVariable long id) {
        return tickets.get(users.resolve(auth), id);
    }

    @GetMapping("/tickets/{id}/history")
    public List<TicketHistoryEntry> history(Authentication auth, @PathVariable long id) {
        return tickets.history(users.resolve(auth), id);
    }

    @PostMapping("/tickets/{id}/comentarios")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void comment(Authentication auth, @PathVariable long id, @Valid @RequestBody CommentRequest request) {
        tickets.comment(users.resolve(auth), id, request.texto(), request.resolver(), request.inventario());
    }

    @PostMapping("/tickets/{id}/replies")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reply(Authentication auth, @PathVariable long id, @Valid @RequestBody ReplyRequest request) {
        tickets.reply(users.resolve(auth), id, request.text());
    }
}
