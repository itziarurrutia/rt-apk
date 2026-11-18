package es.unavarra.rtapk.rt.demo;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetFechaP;
import es.unavarra.rtapk.domain.AssetSearchField;
import es.unavarra.rtapk.rt.AssetGateway;
import es.unavarra.rtapk.rt.rest1.Rest1AssetMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Assets de ejemplo en memoria ({@code rt.api-version=demo}), para trabajar sin RT. */
@Component
@ConditionalOnProperty(name = "rt.api-version", havingValue = "demo")
public class DemoAssetGateway implements AssetGateway {

    private final List<Asset> assets;

    public DemoAssetGateway() {
        Rest1AssetMapper mapper = new Rest1AssetMapper();
        this.assets = DemoData.load("assets.txt").stream().map(mapper::toAsset).toList();
    }

    @Override
    public List<Asset> find(AssetSearchField field, String value) {
        Function<Asset, String> getter = switch (field) {
            case INVENTARIO -> Asset::numeroInventario;
            case SERIE -> Asset::numeroSerie;
            case USUARIO_WINDOWS -> Asset::usuarioWindows;
        };
        return assets.stream().filter(a -> value.equalsIgnoreCase(getter.apply(a))).toList();
    }

    /** Pass_iz inventado a partir del inventario: solo para probar la interfaz. */
    @Override
    public Optional<AssetFechaP> findFechaP(String numeroInventario) {
        return find(AssetSearchField.INVENTARIO, numeroInventario).stream().findFirst()
                .map(a -> new AssetFechaP(a.fechaP(), "demo-" + new StringBuilder(numeroInventario).reverse() + "{X%9}Qa7#kP2"));
    }
}
