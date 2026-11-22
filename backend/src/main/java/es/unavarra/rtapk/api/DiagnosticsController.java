package es.unavarra.rtapk.api;

import es.unavarra.rtapk.rt.rest1.Rest1Diagnostics;
import es.unavarra.rtapk.security.CurrentUser;
import es.unavarra.rtapk.security.CurrentUserResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Diagnóstico de la conexión con RT (solo agentes). */
@RestController
public class DiagnosticsController {

    private final ObjectProvider<Rest1Diagnostics> diagnostics;
    private final CurrentUserResolver users;

    public DiagnosticsController(ObjectProvider<Rest1Diagnostics> diagnostics, CurrentUserResolver users) {
        this.diagnostics = diagnostics;
        this.users = users;
    }

    /** Qué rutas de RT REST 1.0 responden. Vacío si la aplicación no usa REST 1.0 (p. ej. en modo demo). */
    @GetMapping("/api/diagnostico/rt")
    public List<Rest1Diagnostics.Check> rt(Authentication auth) {
        CurrentUser user = users.resolve(auth);
        if (!user.agent()) {
            throw new AccessDeniedException("Solo los agentes pueden ver el diagnóstico de RT");
        }
        Rest1Diagnostics d = diagnostics.getIfAvailable();
        return d == null ? List.of() : d.run();
    }
}
