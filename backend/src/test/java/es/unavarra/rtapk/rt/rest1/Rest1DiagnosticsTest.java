package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.rt.RtCredentialsInterceptor;
import es.unavarra.rtapk.rt.RtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Rest1DiagnosticsTest {

    private static final String RT = "https://rt.example.org/REST/1.0";
    private static final MediaType TEXT = new MediaType("text", "plain", StandardCharsets.UTF_8);

    @Test
    void reportsWhichRoutesRespond() {
        RestClient.Builder builder = RestClient.builder().baseUrl(RT)
                .requestInterceptor(new RtCredentialsInterceptor("svc", () -> "secreto"));
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        RtProperties properties = new RtProperties("rest1", RT, "/asset", "/fecha_p", "svc", "secreto", null,
                ZoneId.of("Europe/Madrid"), StandardCharsets.UTF_8, Duration.ofSeconds(1), Duration.ofSeconds(1));
        // La búsqueda responde; el resto da el 404 HTML de RT 6
        server.expect(manyTimes(), requestTo(startsWith(RT + "/search/ticket")))
                .andRespond(withSuccess("RT/6.0.2 200 Ok\n\nNo matching results.\n", TEXT));
        server.expect(manyTimes(), requestTo(startsWith(RT + "/")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_HTML)
                        .body("<html><head><title>Page not found</title></head></html>"));

        List<Rest1Diagnostics.Check> checks =
                new Rest1Diagnostics(new Rest1Client(builder.build(), properties), properties).run();

        assertThat(checks).hasSize(7);
        assertThat(checks.getFirst().ok()).isTrue();
        assertThat(checks.getFirst().detail()).isEqualTo("RT/6.0.2 200 Ok");
        assertThat(checks.subList(1, 7)).allSatisfy(c -> {
            assertThat(c.ok()).isFalse();
            assertThat(c.detail()).startsWith("404 Page not found");
        });
        assertThat(checks).noneSatisfy(c -> assertThat(c.detail()).contains("secreto"));
    }

    @Test
    void longErrorsAreShortened() {
        assertThat(Rest1Diagnostics.summary("x".repeat(500))).hasSize(161).endsWith("…");
        assertThat(Rest1Diagnostics.summary("Connection\n refused")).isEqualTo("Connection refused");
    }
}
