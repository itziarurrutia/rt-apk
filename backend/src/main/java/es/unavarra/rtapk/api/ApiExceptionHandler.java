package es.unavarra.rtapk.api;

import es.unavarra.rtapk.domain.AssetNotFoundException;
import es.unavarra.rtapk.domain.AssetServiceErrorException;
import es.unavarra.rtapk.domain.InvalidSearchException;
import es.unavarra.rtapk.domain.TicketNotFoundException;
import es.unavarra.rtapk.domain.TicketPartiallyUpdatedException;
import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.service.InvalidRequestException;
import es.unavarra.rtapk.service.MissingEmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Errores de la API en formato RFC 9457 (application/problem+json). */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({TicketNotFoundException.class, AssetNotFoundException.class})
    ProblemDetail notFound(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({MissingEmailException.class, InvalidRequestException.class})
    ProblemDetail badRequest(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(InvalidSearchException.class)
    ProblemDetail invalidSearch(InvalidSearchException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** RT ha contestado 200 con "# Error: ...": se muestra al usuario el texto tras "Error". */
    @ExceptionHandler(AssetServiceErrorException.class)
    ProblemDetail assetServiceError(AssetServiceErrorException e) {
        log.warn("El servicio de equipos de RT devolvió: {}", e.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
    }

    @ExceptionHandler(RtAccessException.class)
    ProblemDetail rtUnavailable(RtAccessException e) {
        log.error("Error accediendo a RT", e);
        // No se devuelve el detalle interno de RT al cliente
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Request Tracker no está disponible");
    }

    @ExceptionHandler(TicketPartiallyUpdatedException.class)
    ProblemDetail partiallyUpdated(TicketPartiallyUpdatedException e) {
        log.warn(e.getMessage(), e.getCause());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    ProblemDetail notImplemented(UnsupportedOperationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, e.getMessage());
    }
}
