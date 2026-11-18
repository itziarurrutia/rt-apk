package es.unavarra.rtapk.rt;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetFechaP;
import es.unavarra.rtapk.domain.AssetSearchField;

import java.util.List;
import java.util.Optional;

/** Puerto de acceso a los assets de RT. */
public interface AssetGateway {

    /** Assets cuyo campo {@code field} es exactamente {@code value}. Lista vacía si no hay ninguno. */
    List<Asset> find(AssetSearchField field, String value);

    /** Fecha P y Pass_iz del asset; vacío si el asset no existe. */
    Optional<AssetFechaP> findFechaP(String numeroInventario);
}
