package es.unavarra.rtapk.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class Windows11CompatibilityTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Intel(R) Core(TM) i5-8500 CPU @ 3.00GHz",
            "Intel Core i7-10700",
            "Intel(R) Core(TM) i5-1235U",
            "i3-12100T",
            "Intel Core i9 14900K",
            "Intel(R) Core(TM) Ultra 7 155H",
            "AMD Ryzen 5 3600 6-Core Processor",
            "AMD Ryzen 7 PRO 5850U",
            "AMD Ryzen 5 2600"})
    void compatible(String processor) {
        assertThat(Windows11Compatibility.isCompatible(processor)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Intel(R) Core(TM) i5-7500 CPU @ 3.40GHz",
            "Intel Core i7-4790",
            "Intel(R) Core(TM) i5-6200U",
            "AMD Ryzen 5 2500U",
            "AMD Ryzen 3 1200",
            "Intel(R) Pentium(R) CPU G4560",
            "Apple M1",
            " ",
            "desconocido"})
    void notCompatible(String processor) {
        assertThat(Windows11Compatibility.isCompatible(processor)).isFalse();
    }
}
