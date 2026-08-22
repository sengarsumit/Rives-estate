package com.example.estate.Rives.estate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

// Simple in-memory fixed-window limiter for the credential-guessing surface
// (signin/signup). In-memory is a deliberate choice, not an oversight: this
// app runs as a single instance with no Redis/shared cache in the stack, and
// adding one just for rate limiting would be exactly the kind of dependency
// rule #31 warns against. If this app is ever horizontally scaled, this
// needs to move to a shared store - noted in implementation.md.
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of("/api/auth/signin", "/api/auth/signup");
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final Map<String, ConcurrentLinkedDeque<Instant>> attemptsByKey = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isLimitedRequest(request) || !isRateLimited(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("message", "Too many attempts. Please try again in a minute.");
        body.put("path", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }

    private boolean isLimitedRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && LIMITED_PATHS.contains(request.getRequestURI());
    }

    private boolean isRateLimited(HttpServletRequest request) {
        String key = request.getRequestURI() + "|" + request.getRemoteAddr();
        ConcurrentLinkedDeque<Instant> attempts = attemptsByKey.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        Instant now = Instant.now();

        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(now.minus(WINDOW))) {
                attempts.pollFirst();
            }
            if (attempts.size() >= MAX_ATTEMPTS) {
                return true;
            }
            attempts.addLast(now);
            return false;
        }
    }
}
