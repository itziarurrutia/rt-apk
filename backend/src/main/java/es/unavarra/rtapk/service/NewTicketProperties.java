package es.unavarra.rtapk.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Colas que se pueden elegir al crear un ticket con el botón "+" ({@code app.new-ticket.queues}), cada una con
 * un texto que explica de qué trata. Sin configurar, las cuatro de apoyo.
 */
@ConfigurationProperties("app.new-ticket")
public record NewTicketProperties(List<QueueOption> queues) {

    /** Cola de RT y de qué trata (se muestra en el formulario). */
    public record QueueOption(String name, String description) {
    }

    public static final List<QueueOption> DEFAULT_QUEUES = List.of(
            new QueueOption("apoyo.aulas", "Incidencias de aulas, salas, proyectores, licencias…"),
            new QueueOption("apoyo.si", "Cerraduras, tarjetas, RT, Office 365, Acrobat, Google Suite…"),
            new QueueOption("apoyo.dpto", "Apoyo a departamentos e institutos de investigación"),
            new QueueOption("apoyo.admon", "Apoyo a personal PTGAS"),
            new QueueOption("sistemas", "Red, DNS, wifi…"),
            new QueueOption("averias", "Mantenimiento microinformático"));

    public NewTicketProperties {
        queues = queues == null || queues.isEmpty() ? DEFAULT_QUEUES
                : queues.stream().filter(q -> q.name() != null && !q.name().isBlank())
                        .map(q -> new QueueOption(q.name().strip(), q.description() == null ? "" : q.description().strip()))
                        .toList();
    }
}
