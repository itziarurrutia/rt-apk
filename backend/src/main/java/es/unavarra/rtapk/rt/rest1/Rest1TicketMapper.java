package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.rt.RtAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Convierte registros REST 1.0 al modelo de dominio. */
public class Rest1TicketMapper {

    private static final Logger log = LoggerFactory.getLogger(Rest1TicketMapper.class);

    /** Formato de fechas de ticket/show, en la zona horaria del usuario de RT: "Mon Mar 4 10:22:11 2024". */
    private static final DateTimeFormatter RT_DISPLAY_DATE =
            DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);
    /**
     * Formato ISO "2024-03-04 09:22:11". En el historial y los adjuntos viene en UTC; en ticket/show
     * (si el usuario de RT tiene ese formato configurado) viene en su zona horaria.
     */
    private static final DateTimeFormatter RT_ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CUSTOM_FIELD_PREFIX = "CF.{";
    /** "Sin fecha" según el idioma del usuario de RT. */
    private static final Set<String> NOT_SET = Set.of("not set", "no establecido");

    private final ZoneId rtZone;

    public Rest1TicketMapper(ZoneId rtZone) {
        this.rtZone = rtZone;
    }

    public Ticket toTicket(Rest1Record record) {
        Map<String, String> f = record.fields();
        return new Ticket(
                parseId(f.get("id")),
                f.get("Subject"),
                f.get("Queue"),
                f.get("Status"),
                f.get("Owner"),
                splitList(f.get("Requestors")),
                parseInteger(f.get("Priority")),
                parseDate(f.get("Created"), rtZone),
                parseDate(f.get("LastUpdated"), rtZone),
                customFields(f));
    }

    TicketHistoryEntry toHistoryEntry(Rest1Record record) {
        Map<String, String> f = record.fields();
        return new TicketHistoryEntry(
                parseId(f.get("id")),
                f.get("Type"),
                f.get("Creator"),
                parseDate(f.get("Created")),
                f.get("Description"),
                f.get("Content"));
    }

    /** Acepta "ticket/123" o "123". */
    static long parseId(String raw) {
        if (raw == null) {
            throw new RtAccessException("Registro de RT sin id");
        }
        String number = raw.substring(raw.lastIndexOf('/') + 1).strip();
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            throw new RtAccessException("Id de RT no numérico: " + raw, e);
        }
    }

    static List<String> splitList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    private static Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Fecha de RT; las ISO se interpretan en UTC (historial, adjuntos). */
    OffsetDateTime parseDate(String raw) {
        return parseDate(raw, ZoneOffset.UTC);
    }

    OffsetDateTime parseDate(String raw, ZoneId isoZone) {
        if (raw == null || raw.isBlank() || NOT_SET.contains(raw.strip().toLowerCase())) {
            return null;
        }
        String value = raw.strip().replaceAll("\\s+", " ");
        try {
            if (Character.isDigit(value.charAt(0))) {
                return LocalDateTime.parse(value, RT_ISO_DATE).atZone(isoZone).toOffsetDateTime();
            }
            return LocalDateTime.parse(value, RT_DISPLAY_DATE).atZone(rtZone).toOffsetDateTime();
        } catch (DateTimeParseException e) {
            log.warn("Fecha de RT con formato no reconocido: '{}'", raw);
            return null;
        }
    }

    private static Map<String, String> customFields(Map<String, String> fields) {
        Map<String, String> result = new LinkedHashMap<>();
        fields.forEach((key, value) -> {
            if (key.startsWith(CUSTOM_FIELD_PREFIX) && key.endsWith("}")) {
                result.put(key.substring(CUSTOM_FIELD_PREFIX.length(), key.length() - 1), value);
            }
        });
        return result;
    }
}
