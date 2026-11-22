package es.unavarra.rtapk.rt.rest1;

import es.unavarra.rtapk.domain.Asset;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static es.unavarra.rtapk.rt.rest1.Fixtures.load;
import static org.assertj.core.api.Assertions.assertThat;

class Rest1AssetMapperTest {

    private final Rest1ResponseParser parser = new Rest1ResponseParser();
    private final Rest1AssetMapper mapper = new Rest1AssetMapper();

    @Test
    void mapsRealAssetResponse() {
        Rest1Response response = parser.parse(load("asset.txt"));

        assertThat(response.dataRecords()).hasSize(1);
        // Claves con espacios y valores que contienen ':'
        assertThat(response.dataRecords().getFirst().fields())
                .containsEntry("Tipo cuenta Windows", "DA")
                .containsEntry("Uso", "Sin especificar")
                .containsEntry("CF.{Uso}", "II.a: Personal PDI")
                .containsEntry("Asset config", "");
        Asset asset = mapper.toAsset(response.dataRecords().getFirst());

        assertThat(asset).isEqualTo(new Asset(
                1908964,
                "78763",
                "1236718",
                "TTL",
                "TeknoPro Q870",
                "dyc.a.unavarra.es (DA)",
                "172.18.215.244",
                "30C599728B91",
                LocalDate.of(2026, 11, 6),
                LocalDate.of(2030, 12, 21),
                "2026/01/31*",
                "carlos.lopez",
                "assets.sobremesas-dpto",
                "Sobremesa",
                "dpto",
                null,
                null,
                "Windows 11",
                false,
                "Pinos",
                "001002",
                "eim-dpt-78763",
                "eim-dpt-78763"));
    }

    @Test
    void hostnameIsTheSubjectExceptForAdmonDesktops() {
        assertThat(Rest1AssetMapper.hostname("assets.sobremesas-dpto", "eim-dpt-78763")).isEqualTo("eim-dpt-78763");
        assertThat(Rest1AssetMapper.hostname("assets.portatiles", "eim-port-78801")).isEqualTo("eim-port-78801");
        assertThat(Rest1AssetMapper.hostname("assets.sobremesas-admon", "pcger70333-gerencia")).isEqualTo("pcger70333");
        assertThat(Rest1AssetMapper.hostname("assets.sobremesas-admon", "pcger70333")).isEqualTo("pcger70333");
        assertThat(Rest1AssetMapper.hostname("assets.sobremesas-admon", null)).isNull();
    }

    @Test
    void buildingNameIsTheTextAfterTheDash() {
        assertThat(Rest1AssetMapper.buildingName("113 - Pinos")).isEqualTo("Pinos");
        assertThat(Rest1AssetMapper.buildingName("120-Aulario")).isEqualTo("Aulario");
        assertThat(Rest1AssetMapper.buildingName("Sin codigo")).isEqualTo("Sin codigo");
        assertThat(Rest1AssetMapper.buildingName("113 - ")).isNull();
        assertThat(Rest1AssetMapper.buildingName(null)).isNull();
    }

    @Test
    void emptyCustomFieldsAreNull() {
        Rest1Response response = parser.parse("RT/4.4.4 200 Ok\n\nid: ticket/1\nCF.{Marca}: \nCF.{Garantia}:\n");

        Asset asset = mapper.toAsset(response.dataRecords().getFirst());

        assertThat(asset.marca()).isNull();
        assertThat(asset.garantia()).isNull();
        assertThat(asset.numeroInventario()).isNull();
    }

    @Test
    void parsesDatesWithSlashesDashesAndTrailingMarks() {
        assertThat(Rest1AssetMapper.parseDate("2026/11/06")).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(Rest1AssetMapper.parseDate("2026-11-04")).isEqualTo(LocalDate.of(2026, 11, 4));
        assertThat(Rest1AssetMapper.parseDate("2026/1/31*")).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(Rest1AssetMapper.parseDate("No establecido")).isNull();
        assertThat(Rest1AssetMapper.parseDate("2026/02/30")).isNull();
    }
}
