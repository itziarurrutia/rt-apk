package es.unavarra.rtapk.service;

import es.unavarra.rtapk.domain.Asset;
import es.unavarra.rtapk.domain.AssetMatch;
import es.unavarra.rtapk.domain.AssetNotFoundException;
import es.unavarra.rtapk.domain.InvalidSearchException;
import es.unavarra.rtapk.domain.PassIz;
import es.unavarra.rtapk.rt.demo.DemoAssetGateway;
import es.unavarra.rtapk.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static es.unavarra.rtapk.domain.AssetSearchField.INVENTARIO;
import static es.unavarra.rtapk.domain.AssetSearchField.SERIE;
import static es.unavarra.rtapk.domain.AssetSearchField.USUARIO_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Usa los datos demo como RT simulado. */
class AssetServiceTest {

    private final AssetService service = new AssetService(new DemoAssetGateway());

    @Test
    void numericTextMatchesInventoryAndSerial() {
        // 78763 es el inventario de un equipo y el número de serie de otro
        List<AssetMatch> matches = service.search(" 78763 ");

        assertThat(matches).extracting(AssetMatch::matchedBy).containsExactly(INVENTARIO, SERIE);
        assertThat(matches).extracting(m -> m.asset().numeroInventario()).containsExactly("78763", "70333");
    }

    @Test
    void textMatchesWindowsUserWithAllTheirAssets() {
        List<AssetMatch> matches = service.search("Carlos.Lopez");

        assertThat(matches).extracting(AssetMatch::matchedBy).containsOnly(USUARIO_WINDOWS);
        assertThat(matches).extracting(m -> m.asset().numeroInventario()).containsExactly("78763", "78801");
    }

    @Test
    void alphanumericSerial() {
        assertThat(service.search("PF4XK21B")).extracting(AssetMatch::matchedBy).containsExactly(SERIE);
    }

    @Test
    void noMatches() {
        assertThat(service.search("nadie")).isEmpty();
    }

    @Test
    void rejectsTextThatCouldAlterTheUrl() {
        assertThatThrownBy(() -> service.search("../ticket/1")).isInstanceOf(InvalidSearchException.class);
        assertThatThrownBy(() -> service.search("")).isInstanceOf(InvalidSearchException.class);
        assertThatThrownBy(() -> service.search("a b")).isInstanceOf(InvalidSearchException.class);
    }

    @Test
    void passIzOnlyForAgents() {
        CurrentUser agent = new CurrentUser("tecnico@example.org", "Técnico", true);
        CurrentUser user = new CurrentUser("ana@example.org", "Ana", false);

        // Pass_iz demo de 78763: "demo-36787{X%9}Qa7#kP2"
        assertThat(service.getPassIz(agent, "78763")).isEqualTo(new PassIz("demo-36787{X%9}Qa7#kP2", "7{X%9}Qa7"));
        assertThatThrownBy(() -> service.getPassIz(user, "78763")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getPassIz(agent, "1")).isInstanceOf(AssetNotFoundException.class);
        assertThatThrownBy(() -> service.getPassIz(agent, "../x")).isInstanceOf(InvalidSearchException.class);
    }

    @Test
    void getByInventario() {
        Asset asset = service.getByInventario("65012");

        assertThat(asset.marca()).isEqualTo("Dell");
        assertThat(asset.usuarioWindows()).isNull();
        assertThatThrownBy(() -> service.getByInventario("1")).isInstanceOf(AssetNotFoundException.class);
    }
}
