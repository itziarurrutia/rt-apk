package es.unavarra.rtapk.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Ticket de RT en el modelo propio de la aplicación, independiente de la versión de la API de RT.
 */
public record Ticket(
        long id,
        String subject,
        String queue,
        String status,
        String owner,
        List<String> requestors,
        Integer priority,
        OffsetDateTime created,
        OffsetDateTime lastUpdated,
        Map<String, String> customFields) {

    public boolean hasRequestor(String email) {
        return email != null && requestors.stream().anyMatch(email::equalsIgnoreCase);
    }
}
