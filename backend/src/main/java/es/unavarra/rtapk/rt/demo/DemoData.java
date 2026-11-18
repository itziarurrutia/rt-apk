package es.unavarra.rtapk.rt.demo;

import es.unavarra.rtapk.rt.rest1.Rest1Record;
import es.unavarra.rtapk.rt.rest1.Rest1ResponseParser;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Carga los datos de ejemplo de {@code classpath:demo/}, escritos en el mismo formato que devuelve RT. */
final class DemoData {

    private DemoData() {
    }

    static List<Rest1Record> load(String name) {
        try {
            String text = new ClassPathResource("demo/" + name).getContentAsString(StandardCharsets.UTF_8);
            return new Rest1ResponseParser().parse(text).dataRecords();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pueden cargar los datos demo " + name, e);
        }
    }
}
