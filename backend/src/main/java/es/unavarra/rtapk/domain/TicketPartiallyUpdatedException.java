package es.unavarra.rtapk.domain;

/** Una operación sobre un ticket se ha hecho solo en parte (p. ej. comentado pero no resuelto). */
public class TicketPartiallyUpdatedException extends RuntimeException {

    public TicketPartiallyUpdatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
