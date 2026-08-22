package com.example.estate.Rives.estate.security;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WsTicketServiceTest {

    private User user(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setRole(Role.USER);
        return user;
    }

    @Test
    void issueThenConsume_returnsTheIssuingUsername() {
        WsTicketService service = new WsTicketService(Duration.ofSeconds(30));
        String ticket = service.issue(user("alice"));

        assertThat(service.consume(ticket)).isEqualTo("alice");
    }

    @Test
    void consume_isSingleUse() {
        WsTicketService service = new WsTicketService(Duration.ofSeconds(30));
        String ticket = service.issue(user("alice"));

        service.consume(ticket);

        assertThat(service.consume(ticket)).isNull();
    }

    @Test
    void consume_unknownTicket_returnsNull() {
        WsTicketService service = new WsTicketService(Duration.ofSeconds(30));

        assertThat(service.consume("not-a-real-ticket")).isNull();
    }

    @Test
    void consume_nullTicket_returnsNull() {
        WsTicketService service = new WsTicketService(Duration.ofSeconds(30));

        assertThat(service.consume(null)).isNull();
    }

    @Test
    void consume_expiredTicket_returnsNull() throws InterruptedException {
        WsTicketService service = new WsTicketService(Duration.ofMillis(20));
        String ticket = service.issue(user("alice"));

        Thread.sleep(50);

        assertThat(service.consume(ticket)).isNull();
    }

    @Test
    void issue_returnsDifferentTicketsForDifferentUsers() {
        WsTicketService service = new WsTicketService(Duration.ofSeconds(30));

        String aliceTicket = service.issue(user("alice"));
        String bobTicket = service.issue(user("bob"));

        assertThat(aliceTicket).isNotEqualTo(bobTicket);
        assertThat(service.consume(aliceTicket)).isEqualTo("alice");
        assertThat(service.consume(bobTicket)).isEqualTo("bob");
    }
}
