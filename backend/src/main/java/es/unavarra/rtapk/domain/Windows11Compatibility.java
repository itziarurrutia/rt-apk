package es.unavarra.rtapk.domain;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compatibilidad aproximada de un procesador con Windows 11, a partir del texto del campo Procesador del asset.
 * Windows 11 exige Intel Core de 8.ª generación o posterior (o Core Ultra) y AMD Ryzen de la serie 2000 (Zen+)
 * o posterior. Cualquier otro procesador, o un texto que no se reconozca, se considera no compatible: en ese
 * caso se usa el sistema operativo que ya tiene el equipo.
 */
public final class Windows11Compatibility {

    /** "i5-8500", "i7 10700", "Core i5-1235U", "i3-12100T" */
    private static final Pattern INTEL_CORE = Pattern.compile("\\bi[3579][- ]?(\\d{4,5})[a-z]*\\b");
    private static final Pattern INTEL_ULTRA = Pattern.compile("\\bcore\\s*ultra\\b|\\bultra\\s*[3579]\\b");
    /** "Ryzen 5 3600", "Ryzen 7 PRO 5850U", "Ryzen 5 2600" */
    private static final Pattern AMD_RYZEN = Pattern.compile("\\bryzen\\s*(?:threadripper\\s*)?[3579]?\\s*(?:pro\\s*)?(\\d{4})([a-z]*)\\b");

    private Windows11Compatibility() {
    }

    public static boolean isCompatible(String processor) {
        if (processor == null || processor.isBlank()) {
            return false;
        }
        String p = processor.toLowerCase(Locale.ROOT);
        if (INTEL_ULTRA.matcher(p).find()) {
            return true;
        }
        Matcher intel = INTEL_CORE.matcher(p);
        if (intel.find()) {
            return intelGeneration(intel.group(1)) >= 8;
        }
        Matcher amd = AMD_RYZEN.matcher(p);
        if (amd.find()) {
            int series = amd.group(1).charAt(0) - '0';
            String suffix = amd.group(2);
            // La serie 2000 de portátil y con gráficos (2200G, 2500U...) es Zen 1: no compatible
            return series >= 3 || (series == 2 && !suffix.matches("g|ge|u|h|hs"));
        }
        return false;
    }

    /** "8500" → 8, "10700" → 10, "1235" (portátil 12.ª gen. con 4 cifras: 1235U) → 12. */
    private static int intelGeneration(String model) {
        if (model.length() == 5) {
            return Integer.parseInt(model.substring(0, 2));
        }
        // 4 cifras: "8500" → 8; los de portátil de 10.ª a 14.ª empiezan por 1 seguido de 0-4 ("1235U")
        if (model.charAt(0) == '1' && model.charAt(1) <= '4') {
            return Integer.parseInt(model.substring(0, 2));
        }
        return model.charAt(0) - '0';
    }
}
