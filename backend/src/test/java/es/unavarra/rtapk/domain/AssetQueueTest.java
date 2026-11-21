package es.unavarra.rtapk.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetQueueTest {

    @Test
    void parsesTypeAndScopeFromTheQueue() {
        assertThat(AssetQueue.parse("assets.sobremesas-dpto")).isEqualTo(new AssetQueue("Sobremesa", "dpto"));
        assertThat(AssetQueue.parse("assets.sobremesas-aulas")).isEqualTo(new AssetQueue("Sobremesa", "aulas"));
        assertThat(AssetQueue.parse("Assets.Portatiles-Admon")).isEqualTo(new AssetQueue("Portátil", "admon"));
        assertThat(AssetQueue.parse("assets.portatil-lab")).isEqualTo(new AssetQueue("Portátil", "lab"));
    }

    @Test
    void missingPartsAreNull() {
        assertThat(AssetQueue.parse("assets.portatiles")).isEqualTo(new AssetQueue("Portátil", null));
        assertThat(AssetQueue.parse("assets.impresoras-dpto")).isEqualTo(new AssetQueue(null, "dpto"));
        assertThat(AssetQueue.parse("apoyo.dpto")).isEqualTo(new AssetQueue(null, null));
        assertThat(AssetQueue.parse(null)).isEqualTo(new AssetQueue(null, null));
    }
}
