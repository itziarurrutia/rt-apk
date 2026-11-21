package es.unavarra.rtapk.domain;

import java.time.LocalDate;

/**
 * Equipo inventariado (asset). En RT es un ticket de una cola {@code assets.*} cuyos datos están en
 * campos personalizados; aquí solo se exponen los que usa la aplicación.
 *
 * @param id     id del ticket de RT que representa el asset
 * @param fechaP valor tal cual viene de RT (puede llevar marcas como un {@code *} final)
 * @param cola   cola de RT del asset (p. ej. assets.sobremesas-dpto)
 * @param tipo   "Portátil" o "Sobremesa", según la cola; null si no lo indica
 * @param ambito "admon", "dpto", "aulas" o "lab", según la cola; null si no lo indica
 * @param procesador          campo Procesador
 * @param memoria             campo Memoria
 * @param sistemaOperativo    campo Sistema Operativo
 * @param compatibleWindows11 si el procesador admite Windows 11 (ver {@link Windows11Compatibility})
 */
public record Asset(
        long id,
        String numeroInventario,
        String numeroSerie,
        String marca,
        String modelo,
        String sufijoDns,
        String ipEth0,
        String macEth0,
        LocalDate fechaCompra,
        LocalDate garantia,
        String fechaP,
        String usuarioWindows,
        String cola,
        String tipo,
        String ambito,
        String procesador,
        String memoria,
        String sistemaOperativo,
        boolean compatibleWindows11) {

    /** Sin cola (datos de prueba o servicios que no la devuelven). */
    public Asset(long id, String numeroInventario, String numeroSerie, String marca, String modelo, String sufijoDns,
                 String ipEth0, String macEth0, LocalDate fechaCompra, LocalDate garantia, String fechaP,
                 String usuarioWindows) {
        this(id, numeroInventario, numeroSerie, marca, modelo, sufijoDns, ipEth0, macEth0, fechaCompra, garantia,
                fechaP, usuarioWindows, null, null, null, null, null, null, false);
    }
}
