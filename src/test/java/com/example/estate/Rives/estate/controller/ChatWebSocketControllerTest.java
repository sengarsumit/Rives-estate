package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.ChatMessageRequest;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private UserRepository userRepository;

    private ChatWebSocketController newController() {
        return new ChatWebSocketController(chatService, userRepository);
    }

    private static User user(String username) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setRole(Role.USER);
        return u;
    }

    @Test
    void send_resolvesUserAndDelegatesToService() {
        User sender = user("alice");
        UUID conversationId = UUID.randomUUID();
        Principal principal = sender::getUsername;
        when(userRepository.findByUsername("alice")).thenReturn(sender);

        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello");

        newController().send(conversationId, request, principal);

        verify(chatService).sendMessage(conversationId, sender, "Hello");
    }

    @Test
    void read_resolvesUserAndDelegatesToService() {
        User reader = user("alice");
        UUID conversationId = UUID.randomUUID();
        Principal principal = reader::getUsername;
        when(userRepository.findByUsername("alice")).thenReturn(reader);

        newController().read(conversationId, principal);

        verify(chatService).markAsRead(conversationId, reader);
    }

    @Test
    void send_unknownPrincipalUsername_throwsAccessDenied() {
        UUID conversationId = UUID.randomUUID();
        Principal principal = () -> "ghost";
        when(userRepository.findByUsername("ghost")).thenReturn(null);

        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello");

        assertThatThrownBy(() -> newController().send(conversationId, request, principal))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void handleException_returnsExceptionMessage() {
        String result = newController().handleException(new IllegalArgumentException("boom"));

        assertThat(result).isEqualTo("boom");
    }
}
