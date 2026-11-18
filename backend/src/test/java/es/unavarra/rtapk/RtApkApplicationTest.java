package es.unavarra.rtapk;

import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.rt.rest1.Rest1RtGateway;
import es.unavarra.rtapk.rt.rest2.Rest2RtGateway;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Arranca el contexto completo y comprueba que la propiedad rt.api-version elige el adaptador. */
class RtApkApplicationTest {

    @Nested
    @SpringBootTest
    class DefaultConfiguration {

        @Autowired
        RtGateway gateway;

        @Test
        void usesRest1ByDefault() {
            assertThat(gateway).isInstanceOf(Rest1RtGateway.class);
        }
    }

    @Nested
    @SpringBootTest(properties = "rt.api-version=rest2")
    class Rest2Configuration {

        @Autowired
        RtGateway gateway;

        @Test
        void switchesToRest2() {
            assertThat(gateway).isInstanceOf(Rest2RtGateway.class);
        }
    }
}
