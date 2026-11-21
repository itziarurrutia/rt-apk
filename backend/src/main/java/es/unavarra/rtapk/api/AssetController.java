package es.unavarra.rtapk.api;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetMatch;
import es.unavarra.rtapk.domain.AssetRequestType;
import es.unavarra.rtapk.domain.UltimoDespliegue;
import es.unavarra.rtapk.domain.PassIz;
import es.unavarra.rtapk.security.CurrentUserResolver;
import es.unavarra.rtapk.service.AssetRequestService;
import es.unavarra.rtapk.service.AssetService;
import es.unavarra.rtapk.service.UltimoDespliegueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assets;
    private final AssetRequestService requests;
    private final UltimoDespliegueService despliegues;
    private final CurrentUserResolver users;

    public AssetController(AssetService assets, AssetRequestService requests, UltimoDespliegueService despliegues,
                           CurrentUserResolver users) {
        this.assets = assets;
        this.requests = requests;
        this.despliegues = despliegues;
        this.users = users;
    }

    /** Busca por número de inventario, número de serie o usuario de Windows con un solo texto. */
    @GetMapping
    public List<AssetMatch> search(Authentication auth, @RequestParam("q") String query) {
        users.resolve(auth);
        return assets.search(query);
    }

    @GetMapping("/{numeroInventario}")
    public Asset get(Authentication auth, @PathVariable String numeroInventario) {
        users.resolve(auth);
        return assets.getByInventario(numeroInventario);
    }

    /** Avería o formateo del equipo: contenido del ticket y si los datos del usuario están salvados. */
    public record AssetRequest(@NotNull AssetRequestType tipo, @NotBlank @Size(max = 10_000) String texto,
                               boolean datosSalvados) {
    }

    public record CreatedTicket(long ticketId) {
    }

    /** Crea un ticket de avería o de formateo del equipo en la cola de averías. */
    @PostMapping("/{numeroInventario}/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedTicket request(Authentication auth, @PathVariable String numeroInventario,
                                 @Valid @RequestBody AssetRequest request) {
        return new CreatedTicket(requests.create(users.resolve(auth), numeroInventario, request.tipo(),
                request.texto(), request.datosSalvados()));
    }

    /** Último despliegue del equipo (último mensaje del script de despliegue); 204 si no tiene. */
    @GetMapping("/{numeroInventario}/ultimo-despliegue")
    public ResponseEntity<UltimoDespliegue> ultimoDespliegue(Authentication auth,
                                                            @PathVariable String numeroInventario) {
        users.resolve(auth);
        return despliegues.find(numeroInventario)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Pass_iz y su fragmento (es una contraseña): no se cachea en el navegador ni en proxies. */
    @GetMapping("/{numeroInventario}/pass-iz")
    public ResponseEntity<PassIz> passIz(Authentication auth, @PathVariable String numeroInventario) {
        PassIz pass = assets.getPassIz(users.resolve(auth), numeroInventario);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pass);
    }
}
