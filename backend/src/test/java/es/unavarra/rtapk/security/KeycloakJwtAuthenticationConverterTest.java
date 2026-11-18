package es.unavarra.rtapk.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void mapsRealmRolesAndEmail() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256")
                .claim("email", "ana@example.org")
                .claim("realm_access", Map.of("roles", List.of("rt-agent", "offline_access")))
                .build();

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth.getName()).isEqualTo("ana@example.org");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_rt-agent", "ROLE_offline_access");
    }

    @Test
    void withoutRealmRolesHasNoAuthorities() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").claim("preferred_username", "ana").build();

        AbstractAuthenticationToken auth = converter.convert(jwt);

        assertThat(auth.getName()).isEqualTo("ana");
        assertThat(auth.getAuthorities()).isEmpty();
    }
}
