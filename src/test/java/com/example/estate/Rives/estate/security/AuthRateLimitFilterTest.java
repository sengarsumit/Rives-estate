package com.example.estate.Rives.estate.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AuthRateLimitFilterTest {

    private final AuthRateLimitFilter filter = new AuthRateLimitFilter();

    private MockHttpServletRequest signinRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/signin");
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    void allowsUpToTheLimitThenRejectsWith429() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(signinRequest("10.0.0.1"), response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(chain, times(5)).doFilter(any(), any());

        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(signinRequest("10.0.0.1"), limited, chain);

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).contains("Too many attempts");
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void tracksEachRemoteAddressIndependently() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(signinRequest("10.0.0.2"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(signinRequest("10.0.0.3"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotLimitUnrelatedEndpoints() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/properties/all");
        request.setRemoteAddr("10.0.0.4");

        for (int i = 0; i < 10; i++) {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        verify(chain, times(10)).doFilter(any(), any());
    }
}
