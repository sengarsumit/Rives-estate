package com.example.estate.Rives.estate.security;

import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WsTicketService wsTicketService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwtCookie(request);
            if (jwt != null && jwtUtil.validateJwtToken(jwt)) {
                String username = jwtUtil.getUsernameFromToken(jwt);
                User user = userRepository.findByUsername(username);
                if (user == null) {
                    throw new UsernameNotFoundException("User not found");
                }
                authenticate(user, jwtUtil.getRoleFromToken(jwt), request);
            } else if (isWebSocketHandshake(request)) {
                // The /ws handshake connects directly to this server (not through
                // the frontend's same-origin proxy), so it never carries the
                // now-host-only auth cookie. See WsTicketService for why.
                String username = wsTicketService.consume(request.getParameter("ticket"));
                User user = username == null ? null : userRepository.findByUsername(username);
                if (user != null) {
                    authenticate(user, user.getRole().toString(), request);
                }
            }
        } catch (Exception e) {
            logger.warn("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private boolean isWebSocketHandshake(HttpServletRequest request) {
        return "/ws".equals(request.getRequestURI());
    }

    private void authenticate(User user, String role, HttpServletRequest request) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String parseJwtCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for(Cookie cookie : request.getCookies())
            {
                if("accessToken".equals(cookie.getName()))
                {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
