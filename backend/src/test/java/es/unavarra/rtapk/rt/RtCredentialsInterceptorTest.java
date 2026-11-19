package es.unavarra.rtapk.rt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RtCredentialsInterceptorTest {

    @Test
    void redactsPasswordInUrls() {
        assertThat(RtCredentialsInterceptor.redact(
                "I/O error on GET request for \"https://rt/REST/1.0/ticket/1?user=svc&pass=p%26ss\": refused"))
                .isEqualTo("I/O error on GET request for \"https://rt/REST/1.0/ticket/1?user=svc&pass=***\": refused");
        assertThat(RtCredentialsInterceptor.redact("https://rt/x?pass=abc&format=l")).isEqualTo("https://rt/x?pass=***&format=l");
        assertThat(RtCredentialsInterceptor.redact("sin url")).isEqualTo("sin url");
        assertThat(RtCredentialsInterceptor.redact(null)).isNull();
    }

    @Test
    void acceptsBaseUrlWithOrWithoutTrailingSlash() {
        assertThat(RtClientConfig.stripTrailingSlash("http://rtserver.prueba.es/REST/1.0/"))
                .isEqualTo("http://rtserver.prueba.es/REST/1.0");
        assertThat(RtClientConfig.stripTrailingSlash("http://rtserver.prueba.es/REST/1.0"))
                .isEqualTo("http://rtserver.prueba.es/REST/1.0");
    }
}
