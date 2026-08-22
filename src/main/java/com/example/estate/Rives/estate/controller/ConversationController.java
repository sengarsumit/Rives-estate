package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.ConversationCreateDTO;
import com.example.estate.Rives.estate.DTO.ConversationDTO;
import com.example.estate.Rives.estate.DTO.ConversationSummaryDTO;
import com.example.estate.Rives.estate.DTO.MessageDTO;
import com.example.estate.Rives.estate.DTO.config.ChatMapper;
import com.example.estate.Rives.estate.model.Conversation;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// REST = reads and conversation setup. Sending messages and marking read
// happen over STOMP (see ChatWebSocketController) so both sides get a live
// broadcast; REST stays the source of truth for the initial page load.
@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ChatService chatService;
    private final ChatMapper chatMapper;

    @PostMapping
    public ResponseEntity<ConversationDTO> createOrGetConversation(
            @Valid @RequestBody ConversationCreateDTO dto,
            @AuthenticationPrincipal User loggedInUser) {
        Conversation conversation = chatService.getOrCreateConversation(dto.getPropertyId(), loggedInUser);
        return ResponseEntity.ok(chatMapper.conversationToDto(conversation));
    }

    @GetMapping
    public ResponseEntity<List<ConversationSummaryDTO>> getMyConversations(@AuthenticationPrincipal User loggedInUser) {
        return ResponseEntity.ok(chatService.getConversationsForUser(loggedInUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User loggedInUser) {
        Conversation conversation = chatService.getConversation(id, loggedInUser);
        return ResponseEntity.ok(chatMapper.conversationToDto(conversation));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<Page<MessageDTO>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal User loggedInUser) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(chatService.getMessages(id, loggedInUser, pageable));
    }
}
