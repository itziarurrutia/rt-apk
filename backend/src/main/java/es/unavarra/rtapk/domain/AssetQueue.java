package es.unavarra.rtapk.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tipo de equipo y ámbito según la cola del asset en RT: "assets.sobremesas-dpto" → Sobremesa / dpto,
 * "assets.portatiles" → Portátil / (sin ámbito).
 *
 * @param tipo   "Portátil" o "Sobremesa"; null si la cola no lo indica
 * @param ambito "admon", "dpto", "aulas" o "lab"; null si la cola no lo indica
 */
public record AssetQueue(String tipo, String ambito) {

    public static final String PORTATIL = "Portátil";
    public static final String SOBREMESA = "Sobremesa";
    private static final Set<String> AMBITOS = Set.of("admon", "dpto", "aulas", "lab");
    private static final String PREFIX = "assets.";

    public static AssetQueue parse(String queue) {
        if (queue == null || !queue.toLowerCase(Locale.ROOT).startsWith(PREFIX)) {
            return new AssetQueue(null, null);
        }
        List<String> parts = Arrays.stream(queue.substring(PREFIX.length()).toLowerCase(Locale.ROOT).split("[-._ ]+"))
                .filter(p -> !p.isBlank())
                .toList();
        String tipo = parts.stream().map(AssetQueue::tipoOf).filter(t -> t != null).findFirst().orElse(null);
        String ambito = parts.stream().filter(AMBITOS::contains).findFirst().orElse(null);
        return new AssetQueue(tipo, ambito);
    }

    private static String tipoOf(String part) {
        if (part.startsWith("portatil")) {
            return PORTATIL;
        }
        if (part.startsWith("sobremesa")) {
            return SOBREMESA;
        }
        return null;
    }
}
