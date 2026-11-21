package es.unavarra.rtapk.rt;

import es.unavarra.rtapk.domain.TicketQuery;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Traduce {@link TicketQuery} a TicketSQL. TicketSQL es el mismo en REST 1.0 y REST2, así que lo
 * comparten ambos adaptadores. Todos los valores se escapan: nunca concatenar texto del usuario a mano.
 */
public final class TicketSqlBuilder {

    private TicketSqlBuilder() {
    }

    public static String build(TicketQuery query) {
        List<String> clauses = new ArrayList<>();
        if (StringUtils.hasText(query.requestor())) {
            clauses.add("Requestor = " + quote(query.requestor()));
        }
        if (!query.statuses().isEmpty()) {
            clauses.add(query.statuses().stream()
                    .map(s -> "Status = " + quote(s))
                    .collect(Collectors.joining(" OR ", "(", ")")));
        }
        if (StringUtils.hasText(query.queue())) {
            clauses.add("Queue = " + quote(query.queue()));
        }
        if (StringUtils.hasText(query.subjectContains())) {
            clauses.add("Subject LIKE " + quote(query.subjectContains()));
        }
        // RT exige una consulta; sin criterios se piden todos los tickets
        return clauses.isEmpty() ? "id > 0" : String.join(" AND ", clauses);
    }

    /** "(Queue = 'apoyo.dpto' AND Created > '5 hours ago' AND Status = '__Active__')" */
    public static String recentActive(String queue, String updatedSince) {
        return "(Queue = " + quote(queue) + " AND Created > " + quote(updatedSince)
                + " AND Status = '__Active__')";
    }

    /** "Status = '__Active__' AND Owner = 'usuario'" */
    public static String activeOwnedBy(String owner) {
        return "Status = '__Active__' AND Owner = " + quote(owner);
    }

    /**
     * Tickets activos del propietario que contienen {@code text} en el asunto; si el texto lleva "@", también
     * en el solicitante: "Status = '__Active__' AND Owner = 'u' AND (Subject LIKE 'x' OR Requestor LIKE 'x')".
     */
    public static String activeOwnedByMatching(String owner, String text) {
        String like = quote(text);
        String match = text.contains("@")
                ? "(Subject LIKE " + like + " OR Requestor LIKE " + like + ")"
                : "Subject LIKE " + like;
        return activeOwnedBy(owner) + " AND " + match;
    }

    static String quote(String value) {
        return "'" + escape(value) + "'";
    }

    /** Escapa un valor para ponerlo dentro de comillas simples de TicketSQL (barra invertida y comilla). */
    public static String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
