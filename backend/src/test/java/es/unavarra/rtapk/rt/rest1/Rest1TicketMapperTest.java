package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.Ticket;
import es.unavarra.rtapk.domain.TicketHistoryEntry;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static es.unavarra.rtapk.rt.rest1.Fixtures.load;
import static org.assertj.core.api.Assertions.assertThat;

class Rest1TicketMapperTest {

    private final Rest1ResponseParser parser = new Rest1ResponseParser();
    private final Rest1TicketMapper mapper = new Rest1TicketMapper(ZoneId.of("Europe/Madrid"));

    @Test
    void mapsTicket() {
        Ticket ticket = mapper.toTicket(parser.parse(load("ticket-show.txt")).dataRecords().getFirst());

        assertThat(ticket.id()).isEqualTo(1234);
        assertThat(ticket.status()).isEqualTo("open");
        assertThat(ticket.priority()).isEqualTo(10);
        assertThat(ticket.requestors()).containsExactly("ana.garcia@example.org", "b.perez@example.org");
        assertThat(ticket.hasRequestor("B.Perez@example.org")).isTrue();
        // "Mon Mar  4" (día con doble espacio) en hora de Madrid, invierno = UTC+1
        assertThat(ticket.created()).isEqualTo(OffsetDateTime.parse("2024-03-04T10:22:11+01:00"));
        assertThat(ticket.customFields())
                .containsEntry("Centro", "Edificio Los Madroños")
                .containsEntry("Tipo de incidencia", "Red");
    }

    @Test
    void mapsHistoryWithUtcDates() {
        List<TicketHistoryEntry> history = parser.parse(load("history-long.txt")).dataRecords().stream()
                .map(mapper::toHistoryEntry).toList();

        assertThat(history).extracting(TicketHistoryEntry::type).containsExactly("Create", "Comment", "Correspond");
        assertThat(history).filteredOn(TicketHistoryEntry::isPublicMessage).hasSize(2);
        assertThat(history.getFirst().created()).isEqualTo(OffsetDateTime.parse("2024-03-04T09:22:11Z"));
    }

    @Test
    void unsetAndUnknownDatesAreNull() {
        assertThat(mapper.parseDate("Not set")).isNull();
        assertThat(mapper.parseDate("No establecido")).isNull();
        assertThat(mapper.parseDate("")).isNull();
        assertThat(mapper.parseDate("ayer por la tarde")).isNull();
    }

    @Test
    void parsesIdsAndLists() {
        assertThat(Rest1TicketMapper.parseId("ticket/77")).isEqualTo(77);
        assertThat(Rest1TicketMapper.parseId("9001")).isEqualTo(9001);
        assertThat(Rest1TicketMapper.splitList(" a@x.org ,b@x.org,, ")).containsExactly("a@x.org", "b@x.org");
        assertThat(Rest1TicketMapper.splitList("")).isEmpty();
    }
}
