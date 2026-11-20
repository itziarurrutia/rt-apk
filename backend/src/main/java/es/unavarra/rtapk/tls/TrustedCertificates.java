package es.unavarra.rtapk.tls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Añade a los certificados de confianza de Java las CA de {@code app.tls.trusted-ca} (rutas separadas por
 * comas, p. ej. {@code classpath:tls/unavarra-ca2.pem}). Java no trae la CA interna de la UPNA
 * (Unavarra CA2), que firma los certificados de Keycloak (baobab2) y de rt5.
 *
 * <p>Se ejecuta al preparar el entorno, antes de crear ningún bean, y cambia el {@link SSLContext} por
 * defecto: así lo usan el cliente de RT y la descarga de claves (JWKS) de Keycloak. Se registra en
 * {@code META-INF/spring.factories}.
 */
public final class TrustedCertificates implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    public static final String PROPERTY = "app.tls.trusted-ca";
    private static final Logger log = LoggerFactory.getLogger(TrustedCertificates.class);

    /** Certificados de Java más los añadidos; null = solo los de Java. */
    private static volatile X509ExtendedTrustManager trustManager;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        String locations = event.getEnvironment().getProperty(PROPERTY, "");
        if (!locations.isBlank()) {
            install(locations, new DefaultResourceLoader());
        }
    }

    /** Instala como SSLContext por defecto los certificados de Java más los de {@code locations}. */
    static synchronized void install(String locations, ResourceLoader loader) {
        try {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);
            int n = 0;
            for (X509Certificate cert : jdkTrustManager().getAcceptedIssuers()) {
                store.setCertificateEntry("jdk-" + n++, cert);
            }
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (String location : locations.split(",")) {
                Resource resource = loader.getResource(location.strip());
                try (InputStream in = resource.getInputStream()) {
                    for (Certificate cert : factory.generateCertificates(in)) {
                        store.setCertificateEntry("extra-" + n++, cert);
                        log.info("Certificado de confianza añadido: {}",
                                ((X509Certificate) cert).getSubjectX500Principal().getName());
                    }
                }
            }
            X509ExtendedTrustManager combined = firstX509(store);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {combined}, null);
            SSLContext.setDefault(context);
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            trustManager = combined;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("No se pudieron cargar los certificados de " + PROPERTY + ": " + locations, e);
        }
    }

    /** Validación por defecto de la aplicación: Java más los certificados añadidos, si los hay. */
    public static X509ExtendedTrustManager defaultTrustManager() {
        X509ExtendedTrustManager current = trustManager;
        return current != null ? current : jdkTrustManager();
    }

    private static X509ExtendedTrustManager jdkTrustManager() {
        try {
            return firstX509(null);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo leer el almacén de certificados de Java", e);
        }
    }

    private static X509ExtendedTrustManager firstX509(KeyStore store) throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        for (TrustManager tm : factory.getTrustManagers()) {
            if (tm instanceof X509ExtendedTrustManager x) {
                return x;
            }
        }
        throw new IllegalStateException("No hay un X509TrustManager disponible");
    }
}
