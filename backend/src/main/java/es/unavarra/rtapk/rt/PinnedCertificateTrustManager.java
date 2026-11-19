package es.unavarra.rtapk.rt;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Acepta el certificado del servidor si su huella SHA-256 está en la lista (aunque esté caducado o su
 * nombre no coincida con el host). Cualquier otro certificado pasa la validación normal de Java.
 *
 * <p>Sirve para un servidor interno con un certificado caducado (rt5): solo se confía en ese certificado
 * exacto, no en cualquiera, así que no abre la puerta a un intermediario.
 */
public final class PinnedCertificateTrustManager extends X509ExtendedTrustManager {

    private final Set<String> pins;
    private final X509ExtendedTrustManager defaults;

    public PinnedCertificateTrustManager(Set<String> sha256Pins, X509ExtendedTrustManager defaults) {
        this.pins = sha256Pins.stream().map(PinnedCertificateTrustManager::normalize).collect(Collectors.toSet());
        this.defaults = defaults;
    }

    /** SSLContext con la validación de Java más las huellas indicadas (separadas por comas). */
    public static SSLContext sslContext(String sha256Pins) {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init((KeyStore) null);
            X509ExtendedTrustManager defaults = null;
            for (TrustManager tm : factory.getTrustManagers()) {
                if (tm instanceof X509ExtendedTrustManager x) {
                    defaults = x;
                }
            }
            Set<String> pins = Set.of(sha256Pins.split("\\s*,\\s*"));
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {new PinnedCertificateTrustManager(pins, defaults)}, null);
            return context;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo preparar el SSLContext de RT", e);
        }
    }

    /** "38:54:cf:..." o "3854CF..." -> "3854CF..." */
    static String normalize(String pin) {
        return pin.replace(":", "").strip().toUpperCase(Locale.ROOT);
    }

    static String sha256(X509Certificate cert) throws CertificateException {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
        } catch (GeneralSecurityException e) {
            throw new CertificateException(e);
        }
    }

    private boolean pinned(X509Certificate[] chain) throws CertificateException {
        return chain != null && chain.length > 0 && pins.contains(sha256(chain[0]));
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        if (!pinned(chain)) {
            defaults.checkServerTrusted(chain, authType, socket);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        if (!pinned(chain)) {
            defaults.checkServerTrusted(chain, authType, engine);
        }
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (!pinned(chain)) {
            defaults.checkServerTrusted(chain, authType);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        defaults.checkClientTrusted(chain, authType, socket);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        defaults.checkClientTrusted(chain, authType, engine);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        defaults.checkClientTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return defaults.getAcceptedIssuers();
    }
}
