package com.example.estate.Rives.estate.DTO;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ConversationSummaryDTO {
    private UUID id;
    private UUID propertyId;
    private String propertyTitle;
    private UserSummaryDTO otherParticipant;
    private String lastMessagePreview;
    private Instant lastMessageAt;
    private long unreadCount;
}
