package es.unavarra.rtapk.rt;

import es.unavarra.rtapk.domain.TicketQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketSqlBuilderTest {

    @Test
    void combinesAllCriteria() {
        String sql = TicketSqlBuilder.build(
                new TicketQuery("ana@example.org", List.of("new", "open"), "Soporte", "VPN"));

        assertThat(sql).isEqualTo(
                "Requestor = 'ana@example.org' AND (Status = 'new' OR Status = 'open') AND Queue = 'Soporte' AND Subject LIKE 'VPN'");
    }

    @Test
    void withoutCriteriaReturnsAllTickets() {
        assertThat(TicketSqlBuilder.build(new TicketQuery(null, null, " ", null))).isEqualTo("id > 0");
    }

    @Test
    void escapesQuotesToPreventInjection() {
        String sql = TicketSqlBuilder.build(new TicketQuery(null, null, null, "x' OR id > '0"));

        assertThat(sql).isEqualTo("Subject LIKE 'x\\' OR id > \\'0'");
        assertThat(TicketSqlBuilder.quote("a\\b")).isEqualTo("'a\\\\b'");
    }

    @Test
    void searchesOwnedActiveTicketsBySubjectOrAlsoRequestorWithAt() {
        assertThat(TicketSqlBuilder.activeOwnedByMatching("ana", "impresora"))
                .isEqualTo("Status = '__Active__' AND Owner = 'ana' AND Subject LIKE 'impresora'");
        assertThat(TicketSqlBuilder.activeOwnedByMatching("ana", "luis@unavarra.es"))
                .isEqualTo("Status = '__Active__' AND Owner = 'ana' AND "
                        + "(Subject LIKE 'luis@unavarra.es' OR Requestor LIKE 'luis@unavarra.es')");
        assertThat(TicketSqlBuilder.activeOwnedByMatching("ana", "x' OR id > '0"))
                .isEqualTo("Status = '__Active__' AND Owner = 'ana' AND Subject LIKE 'x\\' OR id > \\'0'");
    }
}
