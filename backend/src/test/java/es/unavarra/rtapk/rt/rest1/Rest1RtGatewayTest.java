package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketNotFoundException;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.domain.TicketRef;
import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests de contrato del adaptador REST 1.0 contra un RT simulado. El adaptador REST2 deberá
 * cumplir los mismos escenarios.
 */
class Rest1RtGatewayTest {

    private static final String RT = "https://rt.example.org";
    private static final MediaType TEXT_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private MockRestServiceServer server;
    private Rest1RtGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(RT);
        server = MockRestServiceServer.bindTo(builder).build();
        RtProperties properties = new RtProperties("rest1", RT, "/REST/1.0/assets", "/REST/1.0/fecha_p", "svc-app", "secreto", null,
                ZoneId.of("Europe/Madrid"), StandardCharsets.UTF_8, Duration.ofSeconds(1), Duration.ofSeconds(1));
        gateway = new Rest1RtGateway(new Rest1Client(builder.build(), properties), properties);
    }

    private void expectLogin(String sid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, "RT_SID_example.443=" + sid + "; path=/; secure; HttpOnly");
        server.expect(requestTo(RT + "/REST/1.0/"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of("user", "svc-app", "pass", "secreto")))
                .andRespond(withSuccess("RT/5.0.3 200 Ok\n\n", TEXT_UTF8).headers(headers));
    }

    @Test
    void logsInOnceAndSearchesWithEncodedTicketSql() {
        expectLogin("abc");
        // TicketSQL va completamente codificado (comillas, @, paréntesis, comas)
        server.expect(requestTo(RT + "/REST/1.0/search/ticket"
                        + "?query=Requestor%20%3D%20%27ana.garcia%40example.org%27%20AND%20%28Status%20%3D%20%27open%27%29"
                        + "&orderby=-LastUpdated&format=l"
                        + "&fields=Subject%2CQueue%2CStatus%2COwner%2CRequestors%2CPriority%2CCreated%2CLastUpdated"))
                .andExpect(header(HttpHeaders.COOKIE, "RT_SID_example.443=abc"))
                .andRespond(withSuccess(load("search-long.txt"), TEXT_UTF8));
        server.expect(requestTo(RT + "/REST/1.0/ticket/1234/show"))
                .andExpect(header(HttpHeaders.COOKIE, "RT_SID_example.443=abc"))
                .andRespond(withSuccess(load("ticket-show.txt"), TEXT_UTF8));

        List<Ticket> tickets = gateway.findTickets(
                new TicketQuery("ana.garcia@example.org", List.of("open"), null, null));
        Ticket ticket = gateway.getTicket(1234);

        assertThat(tickets).extracting(Ticket::id).containsExactly(1235L, 1234L);
        assertThat(ticket.subject()).isEqualTo("No puedo acceder a la VPN");
        server.verify();
    }

    @Test
    void reLogsInWhenSessionExpires() {
        expectLogin("old");
        server.expect(requestTo(RT + "/REST/1.0/ticket/1234/show"))
                .andRespond(withSuccess("RT/5.0.3 401 Credentials required\n", TEXT_UTF8));
        expectLogin("new");
        server.expect(requestTo(RT + "/REST/1.0/ticket/1234/show"))
                .andExpect(header(HttpHeaders.COOKIE, "RT_SID_example.443=new"))
                .andRespond(withSuccess(load("ticket-show.txt"), TEXT_UTF8));

        assertThat(gateway.getTicket(1234).id()).isEqualTo(1234);
        server.verify();
    }

    @Test
    void missingTicketThrowsNotFound() {
        expectLogin("abc");
        server.expect(requestTo(RT + "/REST/1.0/ticket/999/show"))
                .andRespond(withSuccess("RT/5.0.3 200 Ok\n\n# Ticket 999 does not exist.\n", TEXT_UTF8));

        assertThatThrownBy(() -> gateway.getTicket(999)).isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void readsHistory() {
        expectLogin("abc");
        server.expect(requestTo(RT + "/REST/1.0/ticket/1234/history?format=l"))
                .andRespond(withSuccess(load("history-long.txt"), TEXT_UTF8));

        assertThat(gateway.getHistory(1234)).hasSize(3);
    }

    @Test
    void sendsReplyWithIndentedMultilineText() {
        expectLogin("abc");
        server.expect(requestTo(RT + "/REST/1.0/ticket/1234/comment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(MultiValueMap.fromSingleValue(Map.of(
                        "content", "id: 1234\nAction: correspond\nText: Gracias,\n ya funciona."))))
                .andRespond(withSuccess("RT/5.0.3 200 Ok\n\n# Message recorded\n", TEXT_UTF8));

        gateway.addReply(1234, "Gracias,\r\nya funciona.");
        server.verify();
    }

    @Test
    void findsRecentActiveTicketsOfQueue() {
        expectLogin("abc");
        server.expect(requestTo(RT + "/REST/1.0/search/ticket?query="
                        + "%28Queue%20%3D%20%27apoyo.dpto%27%20AND%20LastUpdated%20%3E%20%275%20hours%20ago%27"
                        + "%20AND%20Status%20%3D%20%27__Active__%27%29"))
                .andRespond(withSuccess(load("search-short.txt"), TEXT_UTF8));

        assertThat(gateway.findRecentActive("apoyo.dpto", "5 hours ago")).containsExactly(
                new TicketRef(1909547, "Problema con sistema de impresión"),
                new TicketRef(1917113, "Problema con la impresora"),
                new TicketRef(1917617, "pendiente"),
                new TicketRef(1917756, "Lola Ayuda"));
    }

    @Test
    void noRecentTickets() {
        expectLogin("abc");
        server.expect(requestTo(startsWith(RT + "/REST/1.0/search/ticket")))
                .andRespond(withSuccess("RT/4.4.4 200 Ok\n\nNo matching results.\n", TEXT_UTF8));

        assertThat(gateway.findRecentActive("apoyo.dpto", "5 hours ago")).isEmpty();
    }

    @Test
    void readsTicketWithIsoDatesInRtTimeZone() {
        expectLogin("abc");
        server.expect(requestTo(RT + "/REST/1.0/ticket/1909547/show")).andRespond(withSuccess(load("ticket-iso.txt"), TEXT_UTF8));

        Ticket ticket = gateway.getTicket(1909547);

        assertThat(ticket.requestors()).containsExactly("usuario.ejemplo@example.org");
        assertThat(ticket.customFields()).containsEntry("NDepartamento", "Economia");
        assertThat(ticket.lastUpdated()).isEqualTo(OffsetDateTime.parse("2026-11-09T09:07:50+01:00"));
    }

    @Test
    void listsAttachmentsAndReadsContent() {
        expectLogin("abc");
        server.expect(requestTo(RT + "/REST/1.0/ticket/1909547/attachments"))
                .andRespond(withSuccess(load("attachments.txt"), TEXT_UTF8));
        server.expect(requestTo(RT + "/REST/1.0/attachment/17930866"))
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
    void failedLoginIsReported() {
        server.expect(requestTo(RT + "/REST/1.0/"))
                .andRespond(withSuccess("RT/5.0.3 401 Credentials required\n", TEXT_UTF8));

        assertThatThrownBy(() -> gateway.getTicket(1)).isInstanceOf(RtAccessException.class)
                .hasMessageContaining("Login en RT fallido");
    }
}
