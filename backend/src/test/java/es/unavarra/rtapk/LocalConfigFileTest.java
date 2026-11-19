package es.unavarra.rtapk;

import es.unavarra.rtapk.rt.RtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/** Los datos de conexión se pueden leer de un fichero local (rt.yml, fuera de git). */
@SpringBootTest(properties = "RT_CONFIG_FILE=src/test/resources/local/rt-test.yml")
class LocalConfigFileTest {

    @Autowired
    RtProperties properties;

    @Test
    void readsConnectionDataFromLocalFile() {
        assertThat(properties.timeZone()).isEqualTo(ZoneId.of("Atlantic/Canary"));
    }
}
