package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketNotFoundException;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.domain.TicketRef;
import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtCredentialsInterceptor;
import es.unavarra.rtapk.rt.RtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static es.unavarra.rtapk.rt.rest1.Fixtures.load;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests de contrato del adaptador REST 1.0 contra un RT simulado. El adaptador REST2 deberá
 * cumplir los mismos escenarios.
 */
class Rest1RtGatewayTest {

    private static final String RT = "https://rt.example.org/REST/1.0";
    /** Credenciales que se añaden a cada petición (como en el script de PowerShell). */
    private static final String AUTH = "user=svc-app&pass=secreto";
    private static final MediaType TEXT_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private MockRestServiceServer server;
    private Rest1RtGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(RT)
                .requestInterceptor(new RtCredentialsInterceptor("svc-app", () -> "secreto"));
        server = MockRestServiceServer.bindTo(builder).build();
        RtProperties properties = new RtProperties("rest1", RT, "/asset", "/fecha_p", "svc-app", "secreto", null,
                ZoneId.of("Europe/Madrid"), StandardCharsets.UTF_8, Duration.ofSeconds(1), Duration.ofSeconds(1));
        gateway = new Rest1RtGateway(new Rest1Client(builder.build(), properties), properties);
    }

    @Test
    void sendsCredentialsOnEveryRequestAndEncodesTicketSql() {
        // TicketSQL va completamente codificado (comillas, @, paréntesis, comas)
        server.expect(requestTo(RT + "/search/ticket"
                        + "?query=Requestor%20%3D%20%27ana.garcia%40example.org%27%20AND%20%28Status%20%3D%20%27open%27%29"
                        + "&orderby=-LastUpdated&format=l"
                        + "&fields=Subject%2CQueue%2CStatus%2COwner%2CRequestors%2CPriority%2CCreated%2CLastUpdated&" + AUTH))
                .andRespond(withSuccess(load("search-long.txt"), TEXT_UTF8));
        server.expect(requestTo(RT + "/ticket/1234/show?" + AUTH))
                .andRespond(withSuccess(load("ticket-show.txt"), TEXT_UTF8));

        List<Ticket> tickets = gateway.findTickets(
                new TicketQuery("ana.garcia@example.org", List.of("open"), null, null));
        Ticket ticket = gateway.getTicket(1234);

        assertThat(tickets).extracting(Ticket::id).containsExactly(1235L, 1234L);
        assertThat(ticket.subject()).isEqualTo("No puedo acceder a la VPN");
        server.verify();
    }

    @Test
    void rejectedCredentialsAreReported() {
        server.expect(requestTo(RT + "/ticket/1234/show?" + AUTH))
                .andRespond(withSuccess("RT/5.0.3 401 Credentials required\n", TEXT_UTF8));

        assertThatThrownBy(() -> gateway.getTicket(1234)).isInstanceOf(RtAccessException.class)
                .hasMessageContaining("rechazó las credenciales");
    }

    @Test
    void missingTicketThrowsNotFound() {
        server.expect(requestTo(RT + "/ticket/999/show?" + AUTH))
                .andRespond(withSuccess("RT/5.0.3 200 Ok\n\n# Ticket 999 does not exist.\n", TEXT_UTF8));

        assertThatThrownBy(() -> gateway.getTicket(999)).isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void readsHistory() {
        server.expect(requestTo(RT + "/ticket/1234/history?format=l&" + AUTH))
                .andRespond(withSuccess(load("history-long.txt"), TEXT_UTF8));

        assertThat(gateway.getHistory(1234)).hasSize(3);
    }

    @Test
    void sendsReplyWithIndentedMultilineText() {
        server.expect(requestTo(RT + "/ticket/1234/comment?" + AUTH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(MultiValueMap.fromSingleValue(Map.of(
                        "content", "id: 1234\nAction: correspond\nText: Gracias,\n ya funciona."))))
                .andRespond(withSuccess("RT/5.0.3 200 Ok\n\n# Message recorded\n", TEXT_UTF8));

        gateway.addReply(1234, "Gracias,\r\nya funciona.");
        server.verify();
    }

    @Test
    void sendsCommentLikeThePowerShellScript() {
        server.expect(requestTo(RT + "/ticket/1546090/comment2?" + AUTH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(MultiValueMap.fromSingleValue(Map.of("content",
                        "id: 1546090\nAction: comment\nText: El usuario ana.garcia ha visto fecha_p\nContent-Type: text/plain\n"
                                + "Subject: El usuario ana.garcia ha visto fecha_p"))))
                .andRespond(withSuccess("RT/6.0.2 200 Ok\n\n# Comments added\n", TEXT_UTF8));

        gateway.addComment(1546090, "El usuario ana.garcia ha visto fecha_p", "El usuario ana.garcia ha visto fecha_p");
        server.verify();
    }

    @Test
    void findsRecentActiveTicketsOfQueue() {
        server.expect(requestTo(RT + "/search/ticket?query="
                        + "%28Queue%20%3D%20%27apoyo.dpto%27%20AND%20Created%20%3E%20%275%20hours%20ago%27"
                        + "%20AND%20Status%20%3D%20%27__Active__%27%29&" + AUTH))
                .andRespond(withSuccess(load("search-short.txt"), TEXT_UTF8));

        assertThat(gateway.findRecentActive("apoyo.dpto", "5 hours ago")).containsExactly(
                new TicketRef(1909547, "Problema con sistema de impresión"),
                new TicketRef(1917113, "Problema con la impresora"),
                new TicketRef(1917617, "pendiente"),
                new TicketRef(1917756, "Lola Ayuda"));
    }

    @Test
    void findsTicketsByQueryInTheRequestedOrder() {
        server.expect(requestTo(RT + "/search/ticket?query="
                        + "Status%20%3D%20%27__Active__%27%20AND%20Owner%20%3D%20%27ana%27&orderby=-Created&" + AUTH))
                .andRespond(withSuccess(load("search-short.txt"), TEXT_UTF8));

        assertThat(gateway.findByQuery("Status = '__Active__' AND Owner = 'ana'", "-Created"))
                .extracting(TicketRef::id).containsExactly(1909547L, 1917113L, 1917617L, 1917756L);
        server.verify();
    }

    @Test
    void noRecentTickets() {
        server.expect(requestTo(startsWith(RT + "/search/ticket")))
                .andRespond(withSuccess("RT/4.4.4 200 Ok\n\nNo matching results.\n", TEXT_UTF8));

        assertThat(gateway.findRecentActive("apoyo.dpto", "5 hours ago")).isEmpty();
    }

    @Test
    void readsTicketWithIsoDatesInRtTimeZone() {
        server.expect(requestTo(RT + "/ticket/1909547/show?" + AUTH)).andRespond(withSuccess(load("ticket-iso.txt"), TEXT_UTF8));

        Ticket ticket = gateway.getTicket(1909547);

        assertThat(ticket.requestors()).containsExactly("usuario.ejemplo@example.org");
        assertThat(ticket.customFields()).containsEntry("NDepartamento", "Economia");
        assertThat(ticket.lastUpdated()).isEqualTo(OffsetDateTime.parse("2026-11-09T09:07:50+01:00"));
    }

    @Test
    void listsAttachmentsAndReadsContent() {
        server.expect(requestTo(RT + "/ticket/1909547/attachments?" + AUTH))
                .andRespond(withSuccess(load("attachments.txt"), TEXT_UTF8));
        server.expect(requestTo(RT + "/attachment/17930866?" + AUTH))
                .andRespond(withSuccess(load("attachment.txt"), TEXT_UTF8));

        List<AttachmentRef> attachments = gateway.getAttachments(1909547);
        String content = gateway.getAttachmentContent(17930866);

        assertThat(attachments).hasSize(23)
                .contains(new AttachmentRef(17899317, "multipart/alternative"), new AttachmentRef(17930866, "text/plain"));
        assertThat(attachments).filteredOn(AttachmentRef::isTextPlain).hasSize(8);
        // El "-- " de la firma (sangrado) no corta el contenido; se conservan las líneas vacías
        assertThat(content)
                .startsWith("Ticket URL: https://rt.example.org:443/Ticket/Display.html?id=1909547\n\nDavid, sigue")
                .contains("Saludos,\n\n--")
                .contains("\nUsuario Ejemplo\nDepartamento de Economía\n")
                .endsWith("Ya lo he hecho.");
    }

    @Test
    void communicationErrorsDoNotLeakThePassword() {
        server.expect(requestTo(RT + "/ticket/1/show?" + AUTH))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> gateway.getTicket(1)).isInstanceOf(RtAccessException.class)
                .hasMessageContaining("Connection refused")
                .hasMessageNotContaining("secreto")
                .hasNoCause();
    }

    @Test
    void encodesSpecialCharactersInCredentials() {
        RestClient.Builder builder = RestClient.builder().baseUrl(RT)
                .requestInterceptor(new RtCredentialsInterceptor("usuario.rt", () -> "p&ss w%rd+?"));
        MockRestServiceServer otherServer = MockRestServiceServer.bindTo(builder).build();
        RtProperties properties = new RtProperties("rest1", RT, "/asset", "/fecha_p", "usuario.rt", "p&ss w%rd+?", null,
                ZoneId.of("Europe/Madrid"), StandardCharsets.UTF_8, Duration.ofSeconds(1), Duration.ofSeconds(1));
        Rest1RtGateway other = new Rest1RtGateway(new Rest1Client(builder.build(), properties), properties);
        otherServer.expect(requestTo(RT + "/ticket/1234/show?user=usuario.rt&pass=p%26ss%20w%25rd%2B%3F"))
                .andRespond(withSuccess(load("ticket-show.txt"), TEXT_UTF8));

        assertThat(other.getTicket(1234).id()).isEqualTo(1234);
        otherServer.verify();
    }
}
