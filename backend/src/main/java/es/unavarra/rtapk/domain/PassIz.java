package es.unavarra.rtapk.domain;

/**
 * Pass_iz de un asset y el fragmento que se muestra mientras se pulsa el ojo: los 9 caracteres a partir
 * del décimo. Es una contraseña: no se guarda ni se escribe en logs.
 */
public record PassIz(String passIz, String fragmento) {

    static final int START = 9;
    static final int LENGTH = 9;

    public static PassIz of(String passIz) {
        String value = passIz == null ? "" : passIz;
        return new PassIz(value, fragment(value));
    }

    /** Caracteres 10 a 18 (menos si es más corta; vacío si no llega a 10). */
    static String fragment(String passIz) {
        // Por puntos de código, para no partir caracteres fuera del plano básico
        return passIz.codePoints().skip(START).limit(LENGTH)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Override
    public String toString() {
        return "PassIz[***]";
    }
}
