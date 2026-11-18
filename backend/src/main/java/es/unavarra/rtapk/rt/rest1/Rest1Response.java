package es.unavarra.rtapk.rt.rest1;

import java.util.List;
import java.util.stream.Stream;

/**
 * Respuesta REST 1.0 ya parseada. RT casi siempre contesta HTTP 200: el estado real va en la primera
 * línea del cuerpo ({@code RT/5.0.3 200 Ok}, {@code RT/5.0.3 401 Credentials required}...).
 */
public record Rest1Response(String rtVersion, int status, String statusMessage, List<Rest1Record> records) {

    public boolean isOk() {
        return status == 200;
    }

    /** Registros con al menos un campo (descarta bloques que solo traen comentarios). */
    public List<Rest1Record> dataRecords() {
        return records.stream().filter(r -> !r.isEmpty()).toList();
    }

    public Stream<String> allComments() {
        return records.stream().flatMap(r -> r.comments().stream());
    }

    public boolean hasCommentContaining(String text) {
        String needle = text.toLowerCase();
        return allComments().anyMatch(c -> c.toLowerCase().contains(needle));
    }
}
