package es.unavarra.rtapk.api;

import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.security.CurrentUserResolver;
import es.unavarra.rtapk.security.KeycloakJwtAuthenticationConverter;
import es.unavarra.rtapk.security.SecurityConfig;
import es.unavarra.rtapk.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TicketController.class, PublicConfigController.class})
@Import({SecurityConfig.class, KeycloakJwtAuthenticationConverter.class, CurrentUserResolver.class,
        TicketService.class, ApiExceptionHandler.class})
class TicketControllerTest {

    private static final String ANA = "ana.garcia@example.org";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RtGateway rt;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor user(String email) {
        return jwt().jwt(j -> j.claim("preferred_username", "ana.garcia").claim("email", email).claim("name", "Ana García"));
    }

    private static RequestPostProcessor agent(String email) {
        return jwt().jwt(j -> j.claim("preferred_username", "tecnico").claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_rt-agent"));
    }

    private static Ticket ticket(long id, String requestor) {
        return new Ticket(id, "Asunto " + id, "Soporte", "open", "Nobody", List.of(requestor), 0,
                OffsetDateTime.parse("2024-03-04T10:22:11+01:00"), null, Map.of());
    }

    @Test
    void publicConfigDoesNotRequireToken() throws Exception {
        mvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityEnabled").value(true))
                .andExpect(jsonPath("$.issuer").value("http://localhost:8180/realms/rt-mobile"))
                .andExpect(jsonPath("$.clientId").value("rt-web"));
    }

    @Test
    void requiresToken() throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
    }

    @Test
    void userSearchIsRestrictedToOwnTickets() throws Exception {
        when(rt.findTickets(new TicketQuery(ANA, List.of("open"), null, null))).thenReturn(List.of(ticket(1, ANA)));

        mvc.perform(get("/api/tickets").param("status", "open").with(user(ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].requestors[0]").value(ANA));
    }

    @Test
    void agentSearchIsNotRestricted() throws Exception {
        when(rt.findTickets(new TicketQuery(null, List.of(), "Soporte", null))).thenReturn(List.of());

        mvc.perform(get("/api/tickets").param("queue", "Soporte").with(agent("tecnico@example.org")))
                .andExpect(status().isOk());

        verify(rt).findTickets(new TicketQuery(null, List.of(), "Soporte", null));
    }

    @Test
    void otherPeoplesTicketsAreNotFound() throws Exception {
        when(rt.getTicket(7)).thenReturn(ticket(7, "otra@example.org"));

        mvc.perform(get("/api/tickets/7").with(user(ANA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void userHistoryHidesInternalComments() throws Exception {
        when(rt.getTicket(1)).thenReturn(ticket(1, ANA));
        when(rt.getHistory(1)).thenReturn(List.of(
                new TicketHistoryEntry(10, "Create", ANA, null, "Ticket created", "Hola"),
                new TicketHistoryEntry(11, "Comment", "jlopez", null, "Comments added", "interno")));

        mvc.perform(get("/api/tickets/1/history").with(user(ANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("Create"));
    }

    @Test
    void replySignsMessageWithRealAuthor() throws Exception {
        when(rt.getTicket(1)).thenReturn(ticket(1, ANA));

        mvc.perform(post("/api/tickets/1/replies").with(user(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Ya funciona\"}"))
                .andExpect(status().isNoContent());

        verify(rt).addReply(eq(1L), argThat(text -> text.startsWith("Ya funciona") && text.contains("Enviado por Ana García (ana.garcia)")));
    }

    @Test
    void emptyReplyIsRejected() throws Exception {
        mvc.perform(post("/api/tickets/1/replies").with(user(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest());

        verify(rt, never()).addReply(anyLong(), anyString());
    }

    @Test
    void rtFailureIsBadGateway() throws Exception {
        when(rt.getTicket(1)).thenThrow(new RtAccessException("timeout"));

        mvc.perform(get("/api/tickets/1").with(user(ANA)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Request Tracker no está disponible"));
    }

    @Test
    void tokenWithoutUserIsForbidden() throws Exception {
        mvc.perform(get("/api/me").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void userWithoutEmailIsIdentifiedByUsername() throws Exception {
        mvc.perform(get("/api/me").with(jwt().jwt(j -> j.claim("preferred_username", "itziar"))
                        .authorities(new SimpleGrantedAuthority("ROLE_rt-agent"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("itziar"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.agent").value(true));
    }

    @Test
    void userWithoutEmailSeesNoOwnTickets() throws Exception {
        mvc.perform(get("/api/tickets").with(jwt().jwt(j -> j.claim("preferred_username", "itziar"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(rt, never()).findTickets(any());
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        mvc.perform(get("/api/me").with(agent("tecnico@example.org")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tecnico@example.org"))
                .andExpect(jsonPath("$.agent").value(true));
    }
}
