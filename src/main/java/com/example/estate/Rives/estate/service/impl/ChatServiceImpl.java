package com.example.estate.Rives.estate.service.impl;

import com.example.estate.Rives.estate.DTO.ChatNotificationDTO;
import com.example.estate.Rives.estate.DTO.ConversationSummaryDTO;
import com.example.estate.Rives.estate.DTO.MessageDTO;
import com.example.estate.Rives.estate.DTO.ReadReceiptDTO;
import com.example.estate.Rives.estate.DTO.config.ChatMapper;
import com.example.estate.Rives.estate.exception.ApiException;
import com.example.estate.Rives.estate.exception.ResourceNotFoundException;
import com.example.estate.Rives.estate.model.Conversation;
import com.example.estate.Rives.estate.model.Message;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.ConversationRepository;
import com.example.estate.Rives.estate.repository.MessageRepository;
import com.example.estate.Rives.estate.service.ChatService;
import com.example.estate.Rives.estate.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// @Transactional at the class level: unlike ConversationController's REST
// endpoints, ChatWebSocketController's STOMP @MessageMapping methods don't
// run behind the servlet filter chain, so there's no Open-Session-In-View
// wrapping the request - lazy associations (Conversation.property.dealer)
// would otherwise throw LazyInitializationException when reached from a
// STOMP-triggered call. An explicit transaction here covers both callers.
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final int PREVIEW_MAX_LENGTH = 160;
    private static final int MESSAGE_MAX_LENGTH = 2000;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PropertyService propertyService;
    private final ChatMapper chatMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Conversation getOrCreateConversation(UUID propertyId, User requester) {
        Property property = propertyService.getPropertyById(propertyId);
        if (property.getDealer().getId().equals(requester.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot start a conversation about your own property");
        }

        return conversationRepository.findByPropertyIdAndBuyerId(propertyId, requester.getId())
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setProperty(property);
                    conversation.setBuyer(requester);
                    conversation.setLastMessageAt(Instant.now());
                    return conversationRepository.save(conversation);
                });
    }

    @Override
    public Conversation getConversation(UUID conversationId, User requester) {
        Conversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversation, requester);
        return conversation;
    }

    @Override
    public List<ConversationSummaryDTO> getConversationsForUser(User user) {
        return conversationRepository
                .findByBuyerIdOrProperty_DealerIdOrderByLastMessageAtDesc(user.getId(), user.getId())
                .stream()
                .map(conversation -> toSummary(conversation, user))
                .toList();
    }

    @Override
    public Page<MessageDTO> getMessages(UUID conversationId, User requester, Pageable pageable) {
        Conversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversation, requester);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .map(chatMapper::messageToDto);
    }

    @Override
    public MessageDTO sendMessage(UUID conversationId, User sender, String content) {
        if (content == null || content.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message content is required");
        }
        if (content.length() > MESSAGE_MAX_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message content must not exceed " + MESSAGE_MAX_LENGTH + " characters");
        }

        Conversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversation, sender);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now());
        conversation.setLastMessagePreview(preview(saved.getContent()));
        conversationRepository.save(conversation);

        MessageDTO dto = chatMapper.messageToDto(saved);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/messages", dto);

        User recipient = otherParticipant(conversation, sender);
        messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/notifications",
                new ChatNotificationDTO(conversationId, conversation.getProperty().getTitle(), dto.getContent(), dto.getCreatedAt()));

        return dto;
    }

    @Override
    public ReadReceiptDTO markAsRead(UUID conversationId, User reader) {
        Conversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversation, reader);

        List<Message> unread = messageRepository
                .findByConversationIdAndSenderIdNotAndReadAtIsNull(conversationId, reader.getId());
        Instant now = Instant.now();
        unread.forEach(message -> message.setReadAt(now));
        messageRepository.saveAll(unread);

        ReadReceiptDTO receipt = new ReadReceiptDTO(conversationId, reader.getId(), now);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/read", receipt);
        return receipt;
    }

    private ConversationSummaryDTO toSummary(Conversation conversation, User viewer) {
        ConversationSummaryDTO dto = new ConversationSummaryDTO();
        dto.setId(conversation.getId());
        dto.setPropertyId(conversation.getProperty().getId());
        dto.setPropertyTitle(conversation.getProperty().getTitle());
        dto.setOtherParticipant(chatMapper.toSummary(otherParticipant(conversation, viewer)));
        dto.setLastMessagePreview(conversation.getLastMessagePreview());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setUnreadCount(messageRepository
                .countByConversationIdAndSenderIdNotAndReadAtIsNull(conversation.getId(), viewer.getId()));
        return dto;
    }

    private User otherParticipant(Conversation conversation, User viewer) {
        return conversation.getBuyer().getId().equals(viewer.getId())
                ? conversation.getProperty().getDealer()
                : conversation.getBuyer();
    }

    private Conversation getConversationOrThrow(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));
    }

    private void assertParticipant(Conversation conversation, User user) {
        boolean isBuyer = conversation.getBuyer().getId().equals(user.getId());
        boolean isDealer = conversation.getProperty().getDealer().getId().equals(user.getId());
        if (!isBuyer && !isDealer) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }
    }

    private String preview(String content) {
        if (content.length() <= PREVIEW_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, PREVIEW_MAX_LENGTH - 1) + "…";
    }
}
