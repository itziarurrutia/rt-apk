package es.unavarra.rtapk.domain;

/**
 * El servicio de equipos de RT ha respondido bien (200) pero con una línea {@code # Error: ...}. El texto
 * tras "Error" es un mensaje para el usuario y se muestra tal cual.
 */
public class AssetServiceErrorException extends RuntimeException {

    public AssetServiceErrorException(String message) {
        super(message);
    }
}
