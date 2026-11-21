package es.unavarra.rtapk.service;

/** Datos de una solicitud no válidos (se responde 400 con el mensaje). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
