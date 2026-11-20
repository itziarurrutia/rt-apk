package es.unavarra.rtapk.web;

import es.unavarra.rtapk.api.PublicConfigController;
import es.unavarra.rtapk.security.KeycloakJwtAuthenticationConverter;
import es.unavarra.rtapk.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static es.unavarra.rtapk.web.SpaWebConfig.SpaResourceResolver.isClientRoute;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Frontend servido desde el backend (src/test/resources/static hace de Angular compilado). */
@WebMvcTest(PublicConfigController.class)
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class, SpaWebConfig.class})
class SpaWebConfigTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rootServesTheAppWithoutToken() throws Exception {
        // Spring Boot (página de bienvenida) reenvía "/" a index.html; MockMvc no sigue el forward
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("spa-index")));
    }

    @Test
    void angularRoutesServeIndexHtml() throws Exception {
        for (String route : new String[] {"/tickets", "/tickets/1546090", "/activos/78763"}) {
            mvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("spa-index")));
        }
    }

    @Test
    void staticFilesAreServedAndRevalidated() throws Exception {
        mvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("spa-asset")))
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    void missingFilesAreNotReplacedByIndex() throws Exception {
        mvc.perform(get("/main-ABC123.js")).andExpect(status().isNotFound());
    }

    @Test
    void apiStillRequiresTokenAndUnknownApiPathsAre404() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/no-existe").with(jwt())).andExpect(status().isNotFound());
        mvc.perform(get("/api/config")).andExpect(status().isOk());
    }

    @Test
    void otherActuatorEndpointsStayClosed() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
    }

    @Test
    void clientRoutes() {
        assertThat(isClientRoute("")).isTrue();
        assertThat(isClientRoute("tickets/12")).isTrue();
        assertThat(isClientRoute("api/tickets")).isFalse();
        assertThat(isClientRoute("actuator/health")).isFalse();
        assertThat(isClientRoute("swagger-ui/index.html")).isFalse();
        assertThat(isClientRoute("favicon.ico")).isFalse();
    }
}
