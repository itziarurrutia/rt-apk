package es.unavarra.rtapk.domain;

/** Texto de búsqueda con un formato no admitido. */
public class InvalidSearchException extends RuntimeException {

    public InvalidSearchException(String message) {
        super(message);
    }
}
