package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.AssetNotFoundException;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Tickets nuevos desde el botón "+" de las vistas de tickets. Solo en las colas de {@code app.new-ticket.queues};
 * el solicitante es el usuario autenticado (su correo) y el ticket queda sin asignar. El nº de inventario
 * opcional se guarda en el campo Inventario del ticket y se indica en el texto, con los datos del equipo si se
 * encuentra en el inventario.
 */
@Service
public class NewTicketService {

    private static final Pattern INVENTORY = Pattern.compile("^[\\p{L}\\p{N}._\\-]{1,50}$");

    private final RtGateway rt;
    private final AssetService assets;
    private final List<NewTicketProperties.QueueOption> queues;

    public NewTicketService(RtGateway rt, AssetService assets, NewTicketProperties properties) {
        this.rt = rt;
        this.assets = assets;
        this.queues = properties.queues();
    }

    /** Colas en las que se puede crear un ticket, con lo que tratan, en el orden configurado. */
    public List<NewTicketProperties.QueueOption> queues() {
        return queues;
    }

    /** Línea del equipo con sus datos; si no está en el inventario (o RT no responde), solo el número. */
    private String equipment(String inventory) {
        try {
            return AssetRequestService.equipmentLine(assets.getByInventario(inventory));
        } catch (AssetNotFoundException | RtAccessException e) {
            return "Equipo: inventario " + inventory;
        }
    }

    private boolean allowed(String queue) {
        return queues.stream().anyMatch(q -> q.name().equals(queue));
    }

    public long create(CurrentUser user, String queue, String subject, String text, String inventory) {
        if (!user.hasEmail()) {
            throw new MissingEmailException(
                    "Tu usuario no tiene correo electrónico en Keycloak: no se puede abrir el ticket a tu nombre");
        }
        if (!allowed(queue)) {
            throw new InvalidRequestException("Elige una de las colas: "
                    + String.join(", ", queues.stream().map(NewTicketProperties.QueueOption::name).toList()));
        }
        String inv = inventory == null ? "" : inventory.strip();
        if (!inv.isEmpty() && !INVENTORY.matcher(inv).matches()) {
            throw new InvalidRequestException("Nº de inventario no válido (letras, números, '.', '_' o '-')");
        }
        Map<String, String> fields = inv.isEmpty() ? Map.of() : Map.of(Ticket.INVENTORY_FIELD, inv);
        String body = text.strip()
                + (inv.isEmpty() ? "" : "\n\n" + equipment(inv))
                + "\n\n-- \nCreado por " + user.name() + " (" + user.username() + ") desde rt-mobile";
        return rt.createTicket(queue, subject.strip(), user.email(), null, fields, body);
    }
}
