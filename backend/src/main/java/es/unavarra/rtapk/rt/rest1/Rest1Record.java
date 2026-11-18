package es.unavarra.rtapk.rt.rest1;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bloque {@code clave: valor} de una respuesta REST 1.0. Las claves conservan el orden y el formato de RT
 * (p. ej. {@code CF.{Centro}}); los valores multilínea ya vienen sin la sangría de continuación.
 *
 * @param comments líneas que no son campos: las que empiezan por {@code #} (sin el {@code #}) y textos sueltos
 */
public record Rest1Record(Map<String, String> fields, List<String> comments) {

    public Optional<String> field(String name) {
        return Optional.ofNullable(fields.get(name));
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }
}
