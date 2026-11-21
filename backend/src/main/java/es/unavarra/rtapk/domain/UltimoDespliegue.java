package es.unavarra.rtapk.domain;

import java.time.OffsetDateTime;

/**
 * Último despliegue registrado en un equipo: el mensaje más reciente que el script de despliegue
 * (usuario de RT {@code app.assets.deployment-user}) dejó en el asset.
 *
 * @param transaccionId transacción de RT del mensaje
 * @param contenido     texto plano del mensaje
 */
public record UltimoDespliegue(long transaccionId, OffsetDateTime fecha, String contenido) {
}
