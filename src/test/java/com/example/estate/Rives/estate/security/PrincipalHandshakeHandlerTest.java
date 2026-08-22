package com.example.estate.Rives.estate.security;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PrincipalHandshakeHandlerTest {

    private final PrincipalHandshakeHandler handler = new PrincipalHandshakeHandler();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private ServerHttpRequest request() {
        return new ServletServerHttpRequest(new MockHttpServletRequest());
    }

    @Test
    void determineUser_authenticatedUser_returnsAuthenticationAsPrincipal() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setRole(Role.USER);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Map<String, Object> attributes = new HashMap<>();
        Principal principal = handler.determineUser(request(), null, attributes);

        assertThat(principal).isNotNull();
        assertThat(principal.getName()).isEqualTo("alice");
    }

    @Test
    void determineUser_noAuthentication_returnsNull() {
        Principal principal = handler.determineUser(request(), null, new HashMap<>());

        assertThat(principal).isNull();
    }

    @Test
    void determineUser_anonymousAuthentication_returnsNull() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        Principal principal = handler.determineUser(request(), null, new HashMap<>());

        assertThat(principal).isNull();
    }
}
