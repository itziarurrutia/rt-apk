package es.unavarra.rtapk.domain;

/** Resultado corto de una búsqueda de tickets en RT ("1909547: Problema con sistema de impresión"). */
public record TicketRef(long id, String subject) {
}
