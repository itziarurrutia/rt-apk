package es.unavarra.rtapk.service;

/** La operación necesita el correo del usuario y su token de Keycloak no lo trae. */
public class MissingEmailException extends RuntimeException {

    public MissingEmailException(String message) {
        super(message);
    }
}
