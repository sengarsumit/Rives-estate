package com.example.estate.Rives.estate.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

class WebSecurityConfigTest {

    @Test
    void corsConfiguration_allowsAllStandardRestMethodsIncludingPatch() {
        WebSecurityConfig config = new WebSecurityConfig();
        // frontendOrigin is normally @Value-injected by Spring; set it
        // directly since this test constructs the config outside a context.
        ReflectionTestUtils.setField(config, "frontendOrigin", "http://localhost:5173");

        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/properties/some-id");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:5173");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}
