package es.unavarra.rtapk.domain;

public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException(String numeroInventario) {
        super("No existe ningún asset con número de inventario " + numeroInventario);
    }
}
