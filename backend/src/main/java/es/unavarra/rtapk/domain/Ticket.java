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

    /** Campo personalizado de RT con el nº de inventario del equipo (tickets de apoyo.dpto). */
    public static final String INVENTORY_FIELD = "Inventario";
    /** Cola de apoyo a departamentos: sus tickets llevan NDepartamento e Inventario. */
    public static final String DEPARTMENT_QUEUE = "apoyo.dpto";

    public boolean inDepartmentQueue() {
        return DEPARTMENT_QUEUE.equalsIgnoreCase(queue);
    }

    /** Nº de inventario (campo personalizado Inventario); null si no tiene. */
    public String inventory() {
        String value = customFields.get(INVENTORY_FIELD);
        return value == null || value.isBlank() ? null : value.strip();
    }

    public boolean hasRequestor(String email) {
        return email != null && requestors.stream().anyMatch(email::equalsIgnoreCase);
    }
}
