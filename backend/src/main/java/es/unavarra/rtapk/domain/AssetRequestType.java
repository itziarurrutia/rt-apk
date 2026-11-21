package es.unavarra.rtapk.domain;

/** Solicitudes que se pueden abrir desde la ficha de un equipo (tickets en la cola de averías). */
public enum AssetRequestType {

    AVERIA,
    FORMATEO;

    /**
     * Asunto del ticket: "Avería en el equipo 78763" o
     * "Formatear y poner imagen de Windows 11 en equipo: 78763".
     *
     * @param operatingSystem sistema operativo del formateo (en una avería no se usa)
     */
    public String subject(String numeroInventario, String operatingSystem) {
        return switch (this) {
            case AVERIA -> "Avería en el equipo " + numeroInventario;
            case FORMATEO -> "Formatear y poner imagen de " + operatingSystem + " en equipo: " + numeroInventario;
        };
    }
}
