package es.unavarra.rtapk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Perfil demo: sin token y sin RT, con los datos de ejemplo. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoProfileTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publicConfigSaysNoLoginIsNeeded() throws Exception {
        mvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityEnabled").value(false));
    }

    @Test
    void searchesAssetsWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/assets").param("q", "78763"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].matchedBy").value("INVENTARIO"))
                .andExpect(jsonPath("$[0].asset.marca").value("TTL"))
                .andExpect(jsonPath("$[0].asset.fechaCompra").value("2026-11-06"))
                .andExpect(jsonPath("$[1].matchedBy").value("SERIE"));
    }

    @Test
    void assetDetailAndErrors() throws Exception {
        mvc.perform(get("/api/assets/78801")).andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("ThinkPad T14 Gen 5"));
        mvc.perform(get("/api/assets/1")).andExpect(status().isNotFound());
        mvc.perform(get("/api/assets").param("q", "a/b")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void passIzIsNotCached() throws Exception {
        mvc.perform(get("/api/assets/78763/pass-iz"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.passIz").value("demo-36787{X%9}Qa7#kP2"))
                .andExpect(jsonPath("$.fragmento").value("7{X%9}Qa7"));
        mvc.perform(get("/api/assets/1/pass-iz")).andExpect(status().isNotFound());
    }

    @Test
    void recentTicketsWithLastMessage() throws Exception {
        mvc.perform(get("/api/tickets/recientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cola").value("apoyo.dpto"))
                .andExpect(jsonPath("$.actualizadosDesde").value("5 hours ago"))
                .andExpect(jsonPath("$.tickets.length()").value(3))
                .andExpect(jsonPath("$.tickets[0].id").value(1909547))
                .andExpect(jsonPath("$.tickets[0].solicitante").value("demo@example.org"))
                .andExpect(jsonPath("$.tickets[0].departamento").value("Economia"))
                .andExpect(jsonPath("$.tickets[0].contenido").value(org.hamcrest.Matchers.startsWith("Ticket URL:")));
    }

    @Test
    void ticketsWorkWithDemoUser() throws Exception {
        mvc.perform(get("/api/me")).andExpect(jsonPath("$.email").value("demo@example.org"));
        mvc.perform(get("/api/tickets").param("status", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/tickets/1917113/replies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Gracias\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tickets/1917113/history"))
                .andExpect(jsonPath("$[2].type").value("Correspond"));
    }
}
