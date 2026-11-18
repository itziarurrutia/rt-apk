package es.unavarra.rtapk.rt;

/** Error al comunicarse con RT o respuesta de RT no esperada. */
public class RtAccessException extends RuntimeException {

    public RtAccessException(String message) {
        super(message);
    }

    public RtAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
