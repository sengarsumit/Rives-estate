package com.example.estate.Rives.estate.service.impl;

import com.example.estate.Rives.estate.DTO.ConversationSummaryDTO;
import com.example.estate.Rives.estate.DTO.MessageDTO;
import com.example.estate.Rives.estate.DTO.ReadReceiptDTO;
import com.example.estate.Rives.estate.DTO.config.ChatMapper;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.exception.ApiException;
import com.example.estate.Rives.estate.exception.ResourceNotFoundException;
import com.example.estate.Rives.estate.model.Conversation;
import com.example.estate.Rives.estate.model.Message;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.ConversationRepository;
import com.example.estate.Rives.estate.repository.MessageRepository;
import com.example.estate.Rives.estate.service.PropertyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private PropertyService propertyService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ChatServiceImpl chatService;

    private User buyer;
    private User dealer;
    private User stranger;
    private Property property;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(conversationRepository, messageRepository, propertyService,
                new ChatMapper(), messagingTemplate);

        dealer = user("dealerbob", Role.DEALER);
        buyer = user("alice", Role.USER);
        stranger = user("carl", Role.USER);

        property = new Property();
        property.setId(UUID.randomUUID());
        property.setTitle("Sea View Villa");
        property.setDealer(dealer);
    }

    private static User user(String username, Role role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setRole(role);
        return u;
    }

    private Conversation conversation() {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setProperty(property);
        conversation.setBuyer(buyer);
        conversation.setCreatedAt(Instant.now());
        conversation.setLastMessageAt(Instant.now());
        return conversation;
    }

    @Test
    void getOrCreateConversation_noExistingConversation_createsNewOne() {
        when(propertyService.getPropertyById(property.getId())).thenReturn(property);
        when(conversationRepository.findByPropertyIdAndBuyerId(property.getId(), buyer.getId()))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        Conversation result = chatService.getOrCreateConversation(property.getId(), buyer);

        assertThat(result.getProperty()).isEqualTo(property);
        assertThat(result.getBuyer()).isEqualTo(buyer);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void getOrCreateConversation_existingConversation_returnsExistingWithoutSaving() {
        Conversation existing = conversation();
        when(propertyService.getPropertyById(property.getId())).thenReturn(property);
        when(conversationRepository.findByPropertyIdAndBuyerId(property.getId(), buyer.getId()))
                .thenReturn(Optional.of(existing));

        Conversation result = chatService.getOrCreateConversation(property.getId(), buyer);

        assertThat(result).isEqualTo(existing);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void getOrCreateConversation_ownProperty_throwsBadRequest() {
        when(propertyService.getPropertyById(property.getId())).thenReturn(property);

        assertThatThrownBy(() -> chatService.getOrCreateConversation(property.getId(), dealer))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("own property");
    }

    @Test
    void getOrCreateConversation_unknownProperty_propagatesNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(propertyService.getPropertyById(unknownId))
                .thenThrow(new ResourceNotFoundException("Property not found with id: " + unknownId));

        assertThatThrownBy(() -> chatService.getOrCreateConversation(unknownId, buyer))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getConversation_participant_returnsIt() {
        Conversation conversation = conversation();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        Conversation result = chatService.getConversation(conversation.getId(), dealer);

        assertThat(result).isEqualTo(conversation);
    }

    @Test
    void getConversation_nonParticipant_throwsAccessDenied() {
        Conversation conversation = conversation();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> chatService.getConversation(conversation.getId(), stranger))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getConversation_unknownConversation_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(conversationRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getConversation(unknownId, buyer))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMessages_participant_returnsMappedPage() {
        Conversation conversation = conversation();
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setSender(buyer);
        message.setContent("Hello");
        message.setCreatedAt(Instant.now());

        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        Pageable pageable = PageRequest.of(0, 30);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(message)));

        Page<MessageDTO> result = chatService.getMessages(conversation.getId(), buyer, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Hello");
    }

    @Test
    void getMessages_nonParticipant_throwsAccessDenied() {
        Conversation conversation = conversation();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> chatService.getMessages(conversation.getId(), stranger, PageRequest.of(0, 30)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getMessages_unknownConversation_throwsNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(conversationRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.getMessages(unknownId, buyer, PageRequest.of(0, 30)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void sendMessage_byBuyer_savesUpdatesConversationAndBroadcastsToTopicAndDealer() {
        Conversation conversation = conversation();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });

        MessageDTO dto = chatService.sendMessage(conversation.getId(), buyer, "Is this still available?");

        assertThat(dto.getContent()).isEqualTo("Is this still available?");
        assertThat(dto.getSenderId()).isEqualTo(buyer.getId());
        assertThat(conversation.getLastMessagePreview()).isEqualTo("Is this still available?");

        verify(conversationRepository).save(conversation);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/conversations/" + conversation.getId() + "/messages"), any(MessageDTO.class));
        verify(messagingTemplate).convertAndSendToUser(
                eq(dealer.getUsername()), eq("/queue/notifications"), any());
    }

    @Test
    void sendMessage_byDealer_notifiesBuyer() {
        Conversation conversation = conversation();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });

        chatService.sendMessage(conversation.getId(), dealer, "Yes, still available");

        verify(messagingTemplate).convertAndSendToUser(
                eq(buyer.getUsername()), eq("/queue/notifications"), any());
    }

    @Test
    void sendMessage_nonParticipant_throwsAccessDeniedAndNeverSaves() {
        Conversation conversation = conversation();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> chatService.sendMessage(conversation.getId(), stranger, "Hi"))
                .isInstanceOf(AccessDeniedException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_blankContent_throwsBadRequestWithoutLookingUpConversation() {
        UUID conversationId = UUID.randomUUID();

        assertThatThrownBy(() -> chatService.sendMessage(conversationId, buyer, "   "))
                .isInstanceOf(ApiException.class);

        verify(conversationRepository, never()).findById(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void sendMessage_tooLongContent_throwsBadRequestWithoutLookingUpConversation() {
        UUID conversationId = UUID.randomUUID();
        String tooLong = "a".repeat(2001);

        assertThatThrownBy(() -> chatService.sendMessage(conversationId, buyer, tooLong))
                .isInstanceOf(ApiException.class);

        verify(conversationRepository, never()).findById(any());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void markAsRead_marksOnlyMessagesFromOtherParticipantAndBroadcasts() {
        Conversation conversation = conversation();
        Message unreadFromDealer = new Message();
        unreadFromDealer.setId(UUID.randomUUID());
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdAndSenderIdNotAndReadAtIsNull(conversation.getId(), buyer.getId()))
                .thenReturn(List.of(unreadFromDealer));

        ReadReceiptDTO receipt = chatService.markAsRead(conversation.getId(), buyer);

        assertThat(unreadFromDealer.getReadAt()).isNotNull();
        assertThat(receipt.getReaderId()).isEqualTo(buyer.getId());
        assertThat(receipt.getConversationId()).isEqualTo(conversation.getId());

        ArgumentCaptor<List<Message>> savedCaptor = ArgumentCaptor.forClass(List.class);
        verify(messageRepository).saveAll(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).containsExactly(unreadFromDealer);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/conversations/" + conversation.getId() + "/read"), eq(receipt));
    }

    @Test
    void markAsRead_nonParticipant_throwsAccessDenied() {
        Conversation conversation = conversation();
        when(conversationRepository.findById(conversation.getId())).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> chatService.markAsRead(conversation.getId(), stranger))
                .isInstanceOf(AccessDeniedException.class);

        verify(messageRepository, never()).saveAll(any());
    }

    @Test
    void getConversationsForUser_returnsSummaryWithOtherParticipantAndUnreadCount() {
        Conversation conversation = conversation();
        conversation.setLastMessagePreview("See you soon");
        when(conversationRepository.findByBuyerIdOrProperty_DealerIdOrderByLastMessageAtDesc(buyer.getId(), buyer.getId()))
                .thenReturn(List.of(conversation));
        when(messageRepository.countByConversationIdAndSenderIdNotAndReadAtIsNull(conversation.getId(), buyer.getId()))
                .thenReturn(3L);

        List<ConversationSummaryDTO> summaries = chatService.getConversationsForUser(buyer);

        assertThat(summaries).hasSize(1);
        ConversationSummaryDTO summary = summaries.get(0);
        assertThat(summary.getOtherParticipant().getUsername()).isEqualTo(dealer.getUsername());
        assertThat(summary.getUnreadCount()).isEqualTo(3L);
        assertThat(summary.getLastMessagePreview()).isEqualTo("See you soon");
    }
}
