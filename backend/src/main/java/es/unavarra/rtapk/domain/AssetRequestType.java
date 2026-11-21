package es.unavarra.rtapk.domain;

/** Solicitudes que se pueden abrir desde la ficha de un equipo (tickets en la cola de averías). */
public enum AssetRequestType {

    AVERIA("Avería en el equipo"),
    FORMATEO("Solicitud de formateo del equipo");

    private final String subjectPrefix;

    AssetRequestType(String subjectPrefix) {
        this.subjectPrefix = subjectPrefix;
    }

    /** "Avería en el equipo 78763" */
    public String subject(String numeroInventario) {
        return subjectPrefix + " " + numeroInventario;
    }
}
