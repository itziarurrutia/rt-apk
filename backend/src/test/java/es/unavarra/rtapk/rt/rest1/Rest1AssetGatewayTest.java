package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetFechaP;
import es.unavarra.rtapk.domain.AssetSearchField;
import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtCredentialsInterceptor;
import es.unavarra.rtapk.rt.RtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;

import static es.unavarra.rtapk.rt.rest1.Fixtures.load;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Rest1AssetGatewayTest {

    private static final String RT = "https://rt.example.org/REST/1.0";
    private static final String AUTH = "?user=svc-app&pass=secreto";
    private static final MediaType TEXT_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private MockRestServiceServer server;
    private Rest1AssetGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(RT)
                .requestInterceptor(new RtCredentialsInterceptor("svc-app", () -> "secreto"));
        server = MockRestServiceServer.bindTo(builder).build();
        RtProperties properties = new RtProperties("rest1", RT, "/asset", "/fecha_p", "svc-app", "secreto", null,
                ZoneId.of("Europe/Madrid"), StandardCharsets.UTF_8, Duration.ofSeconds(1), Duration.ofSeconds(1));
        gateway = new Rest1AssetGateway(new Rest1Client(builder.build(), properties), properties);
    }

    @Test
    void searchesByInventory() {
        server.expect(requestTo(RT + "/asset/78763/default" + AUTH))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(load("asset.txt"), TEXT_UTF8));

        assertThat(gateway.find(AssetSearchField.INVENTARIO, "78763"))
                .extracting(Asset::numeroInventario).containsExactly("78763");
        server.verify();
    }

    @Test
    void searchesBySerialAndWindowsUser() {
        server.expect(requestTo(RT + "/asset/1236718/ns" + AUTH)).andRespond(withSuccess(load("asset.txt"), TEXT_UTF8));
        server.expect(requestTo(RT + "/asset/carlos.lopez/usuario" + AUTH)).andRespond(withSuccess(load("asset.txt"), TEXT_UTF8));

        assertThat(gateway.find(AssetSearchField.SERIE, "1236718")).hasSize(1);
        assertThat(gateway.find(AssetSearchField.USUARIO_WINDOWS, "carlos.lopez")).hasSize(1);
        server.verify();
    }

    @Test
    void noMatchIsEmpty() {
        server.expect(requestTo(RT + "/asset/99999/default" + AUTH))
                .andRespond(withSuccess("RT/4.4.4 200 Ok\n\n# Error: No existe asset\n", TEXT_UTF8));
        server.expect(requestTo(RT + "/asset/nadie/usuario" + AUTH))
                .andRespond(withSuccess("RT/4.4.4 200 Ok\n\n# Error: No existe asset\n", TEXT_UTF8));

        assertThat(gateway.find(AssetSearchField.INVENTARIO, "99999")).isEmpty();
        assertThat(gateway.find(AssetSearchField.USUARIO_WINDOWS, "nadie")).isEmpty();
    }

    @Test
    void readsFechaPAndPassIz() {
        server.expect(requestTo(RT + "/fecha_p/78763" + AUTH)).andRespond(withSuccess(load("fecha-p.txt"), TEXT_UTF8));
        server.expect(requestTo(RT + "/fecha_p/99999" + AUTH))
                .andRespond(withSuccess("RT/4.4.4 200 Ok\n\n# Error: No existe asset\n", TEXT_UTF8));

        // La contraseña lleva caracteres como { } % que no deben alterarse
        assertThat(gateway.findFechaP("78763"))
                .contains(new AssetFechaP("2026/01/31*", "e-o}rt4ueuA%pu4Xg{I6"));
        assertThat(gateway.findFechaP("99999")).isEmpty();
        assertThat(new AssetFechaP("x", "secreto").toString()).doesNotContain("secreto");
    }

    @Test
    void otherErrorsAreReported() {
        server.expect(requestTo(RT + "/asset/78763/default" + AUTH))
                .andRespond(withSuccess("RT/4.4.4 200 Ok\n\n# Error: Base de datos no disponible\n", TEXT_UTF8));

        assertThatThrownBy(() -> gateway.find(AssetSearchField.INVENTARIO, "78763"))
                .isInstanceOf(RtAccessException.class)
                .hasMessageContaining("Base de datos no disponible");
    }
}
