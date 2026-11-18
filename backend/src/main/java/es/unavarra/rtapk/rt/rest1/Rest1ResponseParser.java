package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.rt.RtAccessException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser del formato de texto plano de la API REST 1.0 de RT:
 *
 * <pre>
 * RT/5.0.3 200 Ok
 *
 * # comentario o mensaje de RT
 * id: ticket/123
 * Subject: Asunto
 * Content: primera línea
 *          línea de continuación (sangrada)
 *
 * --
 *
 * id: ticket/124
 * ...
 * </pre>
 *
 * Los bloques se separan con una línea {@code --}. Es una clase sin estado y segura entre hilos.
 */
public class Rest1ResponseParser {

    private static final Pattern STATUS_LINE = Pattern.compile("^RT/(\\S+)\\s+(\\d{3})\\s*(.*)$");
    /**
     * Claves normales ({@code LastUpdated}, {@code Tipo cuenta Windows}) o campos personalizados
     * ({@code CF.{Nombre con espacios}}). La clave termina en el primer ':' ("Uso: II.a: Personal PDI").
     */
    private static final Pattern FIELD_LINE = Pattern.compile("^(CF\\.\\{[^}]*}|[A-Za-z][\\w .\\-]*?):(?: ?(.*))?$");
    private static final String SEPARATOR = "--";

    public Rest1Response parse(String body) {
        if (body == null || body.isBlank()) {
            throw new RtAccessException("Respuesta vacía de RT");
        }
        List<String> lines = body.replace("\r\n", "\n").replace('\r', '\n').lines().toList();

        int first = 0;
        while (first < lines.size() && lines.get(first).isBlank()) {
            first++;
        }
        Matcher status = STATUS_LINE.matcher(first < lines.size() ? lines.get(first).strip() : "");
        if (!status.matches()) {
            throw new RtAccessException("Respuesta de RT sin línea de estado: " + abbreviate(body));
        }

        List<Rest1Record> records = new ArrayList<>();
        List<String> block = new ArrayList<>();
        for (String line : lines.subList(first + 1, lines.size())) {
            // El separador va en la columna 0; un "--" sangrado es texto de un valor (p. ej. firma de correo)
            if (line.stripTrailing().equals(SEPARATOR)) {
                addBlock(records, block);
                block = new ArrayList<>();
            } else {
                block.add(line);
            }
        }
        addBlock(records, block);

        return new Rest1Response(status.group(1), Integer.parseInt(status.group(2)), status.group(3).strip(),
                List.copyOf(records));
    }

    private static void addBlock(List<Rest1Record> records, List<String> lines) {
        Rest1Record record = parseBlock(lines);
        if (!record.isEmpty() || !record.comments().isEmpty()) {
            records.add(record);
        }
    }

    private static Rest1Record parseBlock(List<String> lines) {
        Map<String, String> fields = new LinkedHashMap<>();
        List<String> comments = new ArrayList<>();
        String currentKey = null;
        StringBuilder currentValue = null;
        int pendingBlankLines = 0;

        for (String line : lines) {
            if (line.isBlank()) {
                // Puede ser una línea vacía dentro de un valor multilínea: se decide al ver la siguiente
                pendingBlankLines++;
                continue;
            }
            boolean continuation = Character.isWhitespace(line.charAt(0));
            if (continuation && currentKey != null) {
                currentValue.append("\n".repeat(pendingBlankLines + 1))
                        .append(stripIndent(line, currentKey.length() + 2));
                pendingBlankLines = 0;
                continue;
            }
            if (currentKey != null) {
                fields.put(currentKey, currentValue.toString());
                currentKey = null;
            }
            pendingBlankLines = 0;

            String trimmed = line.strip();
            if (trimmed.startsWith("#")) {
                comments.add(trimmed.substring(1).strip());
                continue;
            }
            Matcher field = FIELD_LINE.matcher(trimmed);
            if (field.matches()) {
                currentKey = field.group(1);
                currentValue = new StringBuilder(field.group(2) == null ? "" : field.group(2));
            } else {
                // p. ej. "No matching results."
                comments.add(trimmed);
            }
        }
        if (currentKey != null) {
            fields.put(currentKey, currentValue.toString());
        }
        return new Rest1Record(fields, List.copyOf(comments));
    }

    /** RT sangra las continuaciones con tantos espacios como "Clave: "; se quita esa sangría si está. */
    private static String stripIndent(String line, int indent) {
        int i = 0;
        while (i < indent && i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i == indent ? line.substring(indent) : line.stripLeading();
    }

    private static String abbreviate(String text) {
        String oneLine = text.strip().replaceAll("\\s+", " ");
        return oneLine.length() > 120 ? oneLine.substring(0, 120) + "..." : oneLine;
    }
}
