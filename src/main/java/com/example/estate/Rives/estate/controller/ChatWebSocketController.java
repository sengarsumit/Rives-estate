package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.ChatMessageRequest;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

// Writes only (send/read) - both broadcast live via ChatService, which owns
// the SimpMessagingTemplate calls as part of the business operation. Reads
// (history, conversation list) go through ConversationController over REST.
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final ChatService chatService;
    private final UserRepository userRepository;

    @MessageMapping("/conversations/{id}/send")
    public void send(@DestinationVariable UUID id, @Payload ChatMessageRequest request, Principal principal) {
        chatService.sendMessage(id, resolveUser(principal), request.getContent());
    }

    @MessageMapping("/conversations/{id}/read")
    public void read(@DestinationVariable UUID id, Principal principal) {
        chatService.markAsRead(id, resolveUser(principal));
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Exception ex) {
        logger.warn("Chat message rejected: {}", ex.getMessage());
        return ex.getMessage();
    }

    private User resolveUser(Principal principal) {
        User user = userRepository.findByUsername(principal.getName());
        if (user == null) {
            throw new AccessDeniedException("Authenticated user not found");
        }
        return user;
    }
}
