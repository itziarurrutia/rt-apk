package es.unavarra.rtapk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** APP_TICKETS_HOURS fija las horas de la vista de tickets activos. */
@SpringBootTest(properties = "APP_TICKETS_HOURS=8")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class TicketHoursConfigTest {

    @Autowired
    MockMvc mvc;

    @Test
    void usesConfiguredHours() throws Exception {
        mvc.perform(get("/api/tickets/recientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualizadosDesde").value("8 hours ago"));
    }
}
