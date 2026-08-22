package com.example.estate.Rives.estate.security;

import com.example.estate.Rives.estate.model.User;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Short-lived, single-use tickets that authenticate the WebSocket handshake
// in place of a cookie. REST calls go through a same-origin Vercel proxy
// (see vercel.json), so the auth cookie is host-only to the frontend's own
// origin and is never sent on the /ws handshake, which still connects
// directly to Render (Vercel cannot proxy a WebSocket upgrade). The frontend
// fetches a ticket over the already-authenticated REST API immediately
// before each (re)connection and passes it as a query param.
// In-memory for the same reason as AuthRateLimitFilter: single instance, no
// shared cache in the stack.
@Component
public class WsTicketService {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    private final Map<String, Entry> ticketsByValue = new ConcurrentHashMap<>();
    private final Duration ttl;

    public WsTicketService() {
        this(DEFAULT_TTL);
    }

    WsTicketService(Duration ttl) {
        this.ttl = ttl;
    }

    public String issue(User user) {
        sweepExpired();
        String ticket = UUID.randomUUID().toString();
        ticketsByValue.put(ticket, new Entry(user.getUsername(), Instant.now().plus(ttl)));
        return ticket;
    }

    // Single-use: an unknown, already-consumed, or expired ticket returns null.
    public String consume(String ticket) {
        if (ticket == null) {
            return null;
        }
        Entry entry = ticketsByValue.remove(ticket);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return entry.username();
    }

    private void sweepExpired() {
        Instant now = Instant.now();
        ticketsByValue.values().removeIf(entry -> entry.expiresAt().isBefore(now));
    }

    private record Entry(String username, Instant expiresAt) {}
}
