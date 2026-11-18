package es.unavarra.rtapk.domain;

import java.util.List;

/**
 * Criterios de búsqueda de tickets. Los nulos o vacíos no filtran.
 *
 * @param requestor si no es nulo, solo tickets en los que este email es solicitante
 */
public record TicketQuery(
        String requestor,
        List<String> statuses,
        String queue,
        String subjectContains) {

    public TicketQuery {
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }

    public TicketQuery withRequestor(String email) {
        return new TicketQuery(email, statuses, queue, subjectContains);
    }
}
