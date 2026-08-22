package com.example.estate.Rives.estate.DTO.config;

import com.example.estate.Rives.estate.DTO.ConversationDTO;
import com.example.estate.Rives.estate.DTO.MessageDTO;
import com.example.estate.Rives.estate.DTO.UserSummaryDTO;
import com.example.estate.Rives.estate.model.Conversation;
import com.example.estate.Rives.estate.model.Message;
import com.example.estate.Rives.estate.model.User;
import org.springframework.stereotype.Component;

// Mapped by hand rather than through ModelMapper: every field here needs a
// nested-object fixup (User -> UserSummaryDTO, Conversation -> its
// property's title/dealer), the same kind of fixup PropertyMapper already
// does manually for imageUrls, so a shared ModelMapper matching strategy
// wouldn't buy anything for a mapping this small.
@Component
public class ChatMapper {

    public MessageDTO messageToDto(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setContent(message.getContent());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setReadAt(message.getReadAt());
        return dto;
    }

    public ConversationDTO conversationToDto(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setPropertyId(conversation.getProperty().getId());
        dto.setPropertyTitle(conversation.getProperty().getTitle());
        dto.setBuyer(toSummary(conversation.getBuyer()));
        dto.setDealer(toSummary(conversation.getProperty().getDealer()));
        dto.setCreatedAt(conversation.getCreatedAt());
        return dto;
    }

    public UserSummaryDTO toSummary(User user) {
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        return dto;
    }
}
