package es.unavarra.rtapk.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sirve el frontend Angular empaquetado en {@code classpath:/static/} (perfil Maven {@code frontend}), en el
 * mismo puerto que la API.
 *
 * <p>Las rutas de Angular ({@code /tickets/123}, {@code /activos}...) no son ficheros: si no existe el
 * fichero pedido y la ruta no es de la API ni lleva extensión, se devuelve {@code index.html} y el router
 * de Angular se encarga. Sin frontend empaquetado (desarrollo con {@code ng serve}) no hace nada: 404.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    static final String LOCATION = "classpath:/static/";
    private static final String INDEX = "static/index.html";
    /** Prefijos que nunca son rutas de Angular. */
    private static final String[] BACKEND_PREFIXES = {"api/", "actuator/", "v3/", "swagger-ui"};

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(LOCATION)
                // Revalidar siempre (304 si no ha cambiado): tras desplegar, el navegador no se queda con un
                // index.html viejo que apunte a ficheros main-XXXX.js que ya no existen
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    static final class SpaResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            if (!resourcePath.isEmpty()) {
                Resource requested = super.getResource(resourcePath, location);
                if (requested != null) {
                    return requested;
                }
            }
            if (!isClientRoute(resourcePath)) {
                return null;
            }
            Resource index = new ClassPathResource(INDEX);
            return index.exists() ? index : null;
        }

        /** "" (raíz) o una ruta sin extensión que no sea del backend. */
        static boolean isClientRoute(String path) {
            for (String prefix : BACKEND_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return false;
                }
            }
            String last = path.substring(path.lastIndexOf('/') + 1);
            return !last.contains(".");
        }
    }
}
