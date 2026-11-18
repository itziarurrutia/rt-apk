package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.rt.RtAccessException;
import org.junit.jupiter.api.Test;

import static es.unavarra.rtapk.rt.rest1.Fixtures.load;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Rest1ResponseParserTest {

    private final Rest1ResponseParser parser = new Rest1ResponseParser();

    @Test
    void parsesStatusLineAndSingleRecord() {
        Rest1Response response = parser.parse(load("ticket-show.txt"));

        assertThat(response.rtVersion()).isEqualTo("5.0.3");
        assertThat(response.isOk()).isTrue();
        assertThat(response.dataRecords()).hasSize(1);
        Rest1Record ticket = response.dataRecords().getFirst();
        assertThat(ticket.fields())
                .containsEntry("id", "ticket/1234")
                .containsEntry("Subject", "No puedo acceder a la VPN")
                .containsEntry("Cc", "")
                .containsEntry("CF.{Tipo de incidencia}", "Red");
    }

    @Test
    void splitsRecordsOnSeparator() {
        Rest1Response response = parser.parse(load("search-long.txt"));

        assertThat(response.dataRecords()).extracting(r -> r.fields().get("id"))
                .containsExactly("ticket/1235", "ticket/1234");
    }

    @Test
    void joinsMultilineValuesKeepingBlankLinesAndRemovingIndent() {
        Rest1Response response = parser.parse(load("history-long.txt"));

        Rest1Record create = response.dataRecords().getFirst();
        assertThat(create.fields().get("Content")).isEqualTo("""
                Hola,

                desde esta mañana la VPN me da error de autenticación.
                Gracias.""");
        assertThat(create.comments()).containsExactly("3/3 (id/9001/total)");
        assertThat(create.fields().get("Creator")).isEqualTo("ana.garcia@example.org");
    }

    @Test
    void keepsNonFieldLinesAsComments() {
        Rest1Response response = parser.parse("""
                RT/5.0.3 200 Ok

                # Ticket 999 does not exist.
                """);

        assertThat(response.dataRecords()).isEmpty();
        assertThat(response.hasCommentContaining("does not exist")).isTrue();
    }

    @Test
    void indentedDashesAreContentNotSeparator() {
        // El "-- " sangrado es la firma de un correo; el separador real va en la columna 0
        Rest1Response response = parser.parse(String.join("\n",
                "RT/4.4.4 200 Ok",
                "",
                "id: attachment/1",
                "Content: Hola",
                "         -- ",
                "         Firma",
                "",
                "--",
                "",
                "id: attachment/2"));

        assertThat(response.dataRecords()).hasSize(2);
        assertThat(response.dataRecords().getFirst().fields()).containsEntry("Content", "Hola\n-- \nFirma");
    }

    @Test
    void noMatchingResultsIsEmpty() {
        Rest1Response response = parser.parse("RT/5.0.3 200 Ok\n\nNo matching results.\n\n");

        assertThat(response.dataRecords()).isEmpty();
        assertThat(response.allComments()).containsExactly("No matching results.");
    }

    @Test
    void parsesErrorStatus() {
        Rest1Response response = parser.parse("RT/5.0.3 401 Credentials required\n");

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.statusMessage()).isEqualTo("Credentials required");
        assertThat(response.records()).isEmpty();
    }

    @Test
    void acceptsWindowsLineEndings() {
        Rest1Response response = parser.parse("RT/4.4.3 200 Ok\r\n\r\nid: ticket/1\r\nSubject: a\r\n  b\r\n");

        assertThat(response.dataRecords().getFirst().fields()).containsEntry("Subject", "a\nb");
    }

    @Test
    void rejectsBodyWithoutStatusLine() {
        assertThatThrownBy(() -> parser.parse("<html>Login</html>"))
                .isInstanceOf(RtAccessException.class)
                .hasMessageContaining("sin línea de estado");
    }
}
