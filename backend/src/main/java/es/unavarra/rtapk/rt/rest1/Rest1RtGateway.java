package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.AttachmentRef;
import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import es.unavarra.rtapk.domain.TicketNotFoundException;
import es.unavarra.rtapk.domain.TicketQuery;
import es.unavarra.rtapk.domain.TicketRef;
import es.unavarra.rtapk.rt.RtAccessException;
import es.unavarra.rtapk.rt.RtGateway;
import es.unavarra.rtapk.rt.RtProperties;
import es.unavarra.rtapk.rt.TicketSqlBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static es.unavarra.rtapk.rt.rest1.Rest1Client.BASE;

/** Adaptador de tickets para la API REST 1.0 de RT ({@code /REST/1.0}), que responde en texto plano. */
@Component
@ConditionalOnProperty(name = "rt.api-version", havingValue = "rest1", matchIfMissing = true)
public class Rest1RtGateway implements RtGateway {

    private static final String LIST_FIELDS = "Subject,Queue,Status,Owner,Requestors,Priority,Created,LastUpdated";
    private static final Pattern SEARCH_LINE = Pattern.compile("^(\\d+):\\s?(.*)$");
    /** Id, nombre (puede llevar paréntesis) y "(tipo/subtipo / tamaño)" al final. */
    private static final Pattern ATTACHMENT_LINE =
            Pattern.compile("^(\\d+):.*\\(([^()\\s/]+/[^()\\s]+)\\s*/\\s*[^()]*\\),?$");

    private final Rest1Client client;
    private final Rest1TicketMapper mapper;

    public Rest1RtGateway(Rest1Client client, RtProperties properties) {
        this.client = client;
        this.mapper = new Rest1TicketMapper(properties.timeZone());
    }

    @Override
    public List<Ticket> findTickets(TicketQuery query) {
        String sql = TicketSqlBuilder.build(query);
        Rest1Response response = client.call(c -> c.get().uri(b -> b.path(BASE + "/search/ticket")
                .queryParam("query", "{query}")
                .queryParam("orderby", "-LastUpdated")
                .queryParam("format", "l")
                .queryParam("fields", "{fields}")
                .build(sql, LIST_FIELDS)));
        return response.dataRecords().stream().map(mapper::toTicket).toList();
    }

    @Override
    public List<TicketRef> findRecentActive(String queue, String updatedSince) {
        String sql = TicketSqlBuilder.recentActive(queue, updatedSince);
        Rest1Response response = client.call(c -> c.get().uri(b -> b.path(BASE + "/search/ticket")
                .queryParam("query", "{query}")
                .build(sql)));
        // Formato corto "1909547: Asunto": al empezar por un número el parser deja esas líneas como comentarios
        return response.allComments()
                .map(SEARCH_LINE::matcher)
                .filter(Matcher::matches)
                .map(m -> new TicketRef(Long.parseLong(m.group(1)), m.group(2).strip()))
                .toList();
    }

    @Override
    public Ticket getTicket(long id) {
        Rest1Response response = client.call(c -> c.get().uri(BASE + "/ticket/{id}/show", id));
        failIfMissing(response, id);
        return response.dataRecords().stream().findFirst().map(mapper::toTicket)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Override
    public List<TicketHistoryEntry> getHistory(long ticketId) {
        Rest1Response response = client.call(c -> c.get().uri(b -> b.path(BASE + "/ticket/{id}/history")
                .queryParam("format", "l")
                .build(ticketId)));
        failIfMissing(response, ticketId);
        return response.dataRecords().stream().map(mapper::toHistoryEntry).toList();
    }

    @Override
    public List<AttachmentRef> getAttachments(long ticketId) {
        Rest1Response response = client.call(c -> c.get().uri(BASE + "/ticket/{id}/attachments", ticketId));
        failIfMissing(response, ticketId);
        String list = response.dataRecords().stream().findFirst()
                .flatMap(r -> r.field("Attachments")).orElse("");
        // Una línea por adjunto: "17930866: (Unnamed) (text/plain / 1.4k),"
        return list.lines()
                .map(String::strip)
                .map(ATTACHMENT_LINE::matcher)
                .filter(Matcher::matches)
                .map(m -> new AttachmentRef(Long.parseLong(m.group(1)), m.group(2)))
                .toList();
    }

    @Override
    public String getAttachmentContent(long attachmentId) {
        Rest1Response response = client.call(c -> c.get().uri(BASE + "/attachment/{id}", attachmentId));
        return response.dataRecords().stream().findFirst().flatMap(r -> r.field("Content")).orElse(null);
    }

    @Override
    public void addReply(long ticketId, String text) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("content", "id: " + ticketId + "\nAction: correspond\nText: " + indentContinuation(text));
        Rest1Response response = client.call(c -> c.post().uri(BASE + "/ticket/{id}/comment", ticketId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form));
        failIfMissing(response, ticketId);
        if (!response.hasCommentContaining("recorded") && !response.hasCommentContaining("added")) {
            throw new RtAccessException("RT no confirmó la respuesta: " + String.join(" | ", response.allComments().toList()));
        }
    }

    /** En REST 1.0 las líneas de continuación de un valor multilínea van sangradas. */
    static String indentContinuation(String text) {
        return text.replace("\r\n", "\n").replace("\n", "\n ");
    }

    private void failIfMissing(Rest1Response response, long id) {
        if (response.hasCommentContaining("does not exist")) {
            throw new TicketNotFoundException(id);
        }
    }
}
