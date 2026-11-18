package es.unavarra.rtapk.api;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetMatch;
import es.unavarra.rtapk.domain.PassIz;
import es.unavarra.rtapk.security.CurrentUserResolver;
import es.unavarra.rtapk.service.AssetService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assets;
    private final CurrentUserResolver users;

    public AssetController(AssetService assets, CurrentUserResolver users) {
        this.assets = assets;
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

    /** Pass_iz y su fragmento (es una contraseña): no se cachea en el navegador ni en proxies. */
    @GetMapping("/{numeroInventario}/pass-iz")
    public ResponseEntity<PassIz> passIz(Authentication auth, @PathVariable String numeroInventario) {
        PassIz pass = assets.getPassIz(users.resolve(auth), numeroInventario);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(pass);
    }
}
