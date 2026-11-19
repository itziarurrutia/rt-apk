package es.unavarra.rtapk.rt;

import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PinnedCertificateTrustManagerTest {

    /** Huella de tls/autofirmado.pem (openssl x509 -noout -fingerprint -sha256). */
    private static final String PIN =
            "00:AD:C0:3B:A1:86:20:53:01:61:9C:E0:F8:64:1E:9B:5D:84:39:78:C1:8B:D4:6F:8C:AB:D4:60:B0:6B:1F:0E";

    @Test
    void acceptsOnlyThePinnedCertificate() throws Exception {
        X509Certificate[] chain = {selfSigned()};

        assertThatCode(() -> trustManager(PIN).checkServerTrusted(chain, "RSA")).doesNotThrowAnyException();
        // Sin la huella, el certificado autofirmado se rechaza como siempre
        assertThatThrownBy(() -> trustManager("00:11:22").checkServerTrusted(chain, "RSA"))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void acceptsFingerprintWithOrWithoutColonsAndAnyCase() throws Exception {
        assertThat(PinnedCertificateTrustManager.sha256(selfSigned()))
                .isEqualTo(PinnedCertificateTrustManager.normalize(PIN.toLowerCase()))
                .isEqualTo(PinnedCertificateTrustManager.normalize(PIN.replace(":", "")));
    }

    @Test
    void buildsSslContextFromCommaSeparatedPins() {
        assertThat(PinnedCertificateTrustManager.sslContext(PIN + ", AA:BB").getProtocol()).isEqualTo("TLS");
    }

    private static PinnedCertificateTrustManager trustManager(String pin) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        X509ExtendedTrustManager defaults = (X509ExtendedTrustManager) Arrays.stream(factory.getTrustManagers())
                .filter(X509ExtendedTrustManager.class::isInstance).findFirst().orElseThrow();
        return new PinnedCertificateTrustManager(Set.of(pin), defaults);
    }

    private static X509Certificate selfSigned() throws Exception {
        try (InputStream in = PinnedCertificateTrustManagerTest.class.getResourceAsStream("/tls/autofirmado.pem")) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }
}
