package es.unavarra.rtapk.domain;

/**
 * Datos de {@code <rt>/fecha_p/<numero_inventario>}. {@code passIz} es una contraseña: solo se pide bajo
 * demanda, no se guarda ni se escribe en logs.
 */
public record AssetFechaP(String fechaP, String passIz) {

    @Override
    public String toString() {
        return "AssetFechaP[fechaP=" + fechaP + ", passIz=***]";
    }
}
