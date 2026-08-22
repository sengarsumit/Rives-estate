package com.example.estate.Rives.estate.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

// Bridges the stateless JWT-cookie auth AuthTokenFilter already establishes
// on the handshake's HTTP request into the STOMP session's Principal. The
// app has no HttpSession (SessionCreationPolicy.STATELESS), so Spring's
// default handshake handler has nothing to read the user from - without
// this, every WebSocket session would be anonymous even with a valid
// accessToken cookie. The handshake request itself is already required to
// be authenticated by WebSecurityConfig's anyRequest().authenticated(), so
// by the time this runs, authentication is expected to be present.
@Component
public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }
}
