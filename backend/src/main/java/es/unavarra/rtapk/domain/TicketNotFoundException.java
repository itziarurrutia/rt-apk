package es.unavarra.rtapk.domain;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(long id) {
        super("El ticket " + id + " no existe");
    }
}
