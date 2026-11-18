package es.unavarra.rtapk.rt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;

@ConfigurationProperties(prefix = "rt")
public record RtProperties(
        String apiVersion,
        String baseUrl,
        String assetsPath,
        String fechaPPath,
        String username,
        String password,
        String passwordFile,
        ZoneId timeZone,
        Charset charset,
        Duration connectTimeout,
        Duration readTimeout) {

    /** Contraseña de RT: el fichero (secret de Podman) tiene prioridad sobre la variable. */
    public String resolvePassword() {
        if (StringUtils.hasText(passwordFile)) {
            try {
                return Files.readString(Path.of(passwordFile)).strip();
            } catch (IOException e) {
                throw new UncheckedIOException("No se puede leer rt.password-file", e);
            }
        }
        return password;
    }
}
