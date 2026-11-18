package es.unavarra.rtapk.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PassIzTest {

    @Test
    void keepsFullValueAndNineCharactersFromTheTenth() {
        // Ejemplo real de RT para el inventario 78763
        PassIz pass = PassIz.of("e-o}rt4ueuA%pu4Xg{I6");

        assertThat(pass.passIz()).isEqualTo("e-o}rt4ueuA%pu4Xg{I6");
        assertThat(pass.fragmento()).isEqualTo("uA%pu4Xg{");
    }

    @Test
    void shortPasswords() {
        assertThat(PassIz.of("123456789abc").fragmento()).isEqualTo("abc");
        assertThat(PassIz.of("123456789").fragmento()).isEmpty();
        assertThat(PassIz.of(null)).isEqualTo(new PassIz("", ""));
    }

    @Test
    void neverPrintsTheValue() {
        assertThat(PassIz.of("e-o}rt4ueuA%pu4Xg{I6").toString()).doesNotContain("uA%").doesNotContain("e-o}");
    }
}
