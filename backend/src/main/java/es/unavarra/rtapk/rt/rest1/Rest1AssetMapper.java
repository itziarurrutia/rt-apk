package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetQueue;
import es.unavarra.rtapk.domain.Windows11Compatibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Convierte un registro REST 1.0 del servicio de assets al modelo de dominio. */
public class Rest1AssetMapper {

    private static final Logger log = LoggerFactory.getLogger(Rest1AssetMapper.class);

    /** Fecha al principio del valor, con / o - ("2026/11/06", "2026-11-04", "2026/01/31*"). */
    private static final Pattern LEADING_DATE = Pattern.compile("^(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})");

    public Asset toAsset(Rest1Record record) {
        Map<String, String> f = record.fields();
        String queue = f.get("Queue") == null || f.get("Queue").isBlank() ? null : f.get("Queue").strip();
        AssetQueue kind = AssetQueue.parse(queue);
        String subject = f.get("Subject") == null || f.get("Subject").isBlank() ? null : f.get("Subject").strip();
        return new Asset(
                Rest1TicketMapper.parseId(f.get("id")),
                cf(f, "Numero de inventario"),
                cf(f, "Numero de serie"),
                cf(f, "Marca"),
                cf(f, "Modelo"),
                cf(f, "Sufijo DNS"),
                cf(f, "Ip eth0"),
                cf(f, "Mac eth0"),
                parseDate(cf(f, "Fecha de compra")),
                parseDate(cf(f, "Garantia")),
                cf(f, "Fecha_p"),
                cf(f, "Usuario Windows"),
                queue,
                kind.tipo(),
                kind.ambito(),
                cf(f, "Procesador"),
                cf(f, "Memoria"),
                cf(f, "Sistema Operativo"),
                Windows11Compatibility.isCompatible(cf(f, "Procesador")),
                buildingName(cf(f, "Edificio")),
                cf(f, "Codigo espacio"),
                subject,
                hostname(queue, subject));
    }

    private static final String ADMON_DESKTOPS = "assets.sobremesas-admon";

    /**
     * Nombre del equipo en red: el subject, salvo en sobremesas-admon, donde es lo anterior al primer "-"
     * ("pcger70333-gerencia" → "pcger70333").
     */
    static String hostname(String queue, String subject) {
        if (subject == null || !ADMON_DESKTOPS.equalsIgnoreCase(queue)) {
            return subject;
        }
        String first = subject.split("-", 2)[0].strip();
        return first.isEmpty() ? null : first;
    }

    /** "113 - Pinos" → "Pinos" (segundo elemento al separar por "-"); sin "-", el valor tal cual. */
    static String buildingName(String raw) {
        if (raw == null) {
            return null;
        }
        String[] parts = raw.split("-", 2);
        String name = (parts.length > 1 ? parts[1] : parts[0]).strip();
        return name.isEmpty() ? null : name;
    }

    /** Campo personalizado {@code CF.{name}}; vacío o en blanco se devuelve como null. */
    private static String cf(Map<String, String> fields, String name) {
        String value = fields.get("CF.{" + name + "}");
        return value == null || value.isBlank() ? null : value.strip();
    }

    static LocalDate parseDate(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher m = LEADING_DATE.matcher(raw.strip());
        if (!m.find()) {
            log.warn("Fecha de asset con formato no reconocido: '{}'", raw);
            return null;
        }
        try {
            return LocalDate.parse(m.group(1) + "-" + pad(m.group(2)) + "-" + pad(m.group(3)), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            log.warn("Fecha de asset no válida: '{}'", raw);
            return null;
        }
    }

    private static String pad(String n) {
        return n.length() == 1 ? "0" + n : n;
    }
}
