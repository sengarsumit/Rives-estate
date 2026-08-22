package com.example.estate.Rives.estate.security;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthTokenFilterTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final WsTicketService wsTicketService = mock(WsTicketService.class);
    private final AuthTokenFilter filter = new AuthTokenFilter();

    {
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(filter, "userRepository", userRepository);
        ReflectionTestUtils.setField(filter, "wsTicketService", wsTicketService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private User user(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void validAccessTokenCookie_authenticatesAsThatUser() throws Exception {
        User alice = user("alice");
        when(jwtUtil.validateJwtToken("good-token")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("good-token")).thenReturn("alice");
        when(jwtUtil.getRoleFromToken("good-token")).thenReturn("USER");
        when(userRepository.findByUsername("alice")).thenReturn(alice);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setCookies(new Cookie("accessToken", "good-token"));

        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
        verify(wsTicketService, org.mockito.Mockito.never()).consume(any());
    }

    @Test
    void wsHandshakeWithValidTicket_authenticatesAsTicketOwner() throws Exception {
        User alice = user("alice");
        when(wsTicketService.consume("valid-ticket")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(alice);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setParameter("ticket", "valid-ticket");

        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
    }

    @Test
    void wsHandshakeWithInvalidTicket_leavesRequestUnauthenticated() throws Exception {
        when(wsTicketService.consume("bad-ticket")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setParameter("ticket", "bad-ticket");

        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ticketParamOnNonWebSocketPath_isIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/properties/all");
        request.setParameter("ticket", "whatever");

        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(wsTicketService, org.mockito.Mockito.never()).consume(any());
    }

    @Test
    void noJwtCookieAndNoTicket_leavesRequestUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");

        filter.doFilterInternal(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
