package com.example.estate.Rives.estate.service;

import com.example.estate.Rives.estate.DTO.ConversationSummaryDTO;
import com.example.estate.Rives.estate.DTO.MessageDTO;
import com.example.estate.Rives.estate.DTO.ReadReceiptDTO;
import com.example.estate.Rives.estate.model.Conversation;
import com.example.estate.Rives.estate.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ChatService {
    Conversation getOrCreateConversation(UUID propertyId, User requester);

    Conversation getConversation(UUID conversationId, User requester);

    List<ConversationSummaryDTO> getConversationsForUser(User user);

    Page<MessageDTO> getMessages(UUID conversationId, User requester, Pageable pageable);

    MessageDTO sendMessage(UUID conversationId, User sender, String content);

    ReadReceiptDTO markAsRead(UUID conversationId, User reader);
}
