package com.example.estate.Rives.estate.DTO;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class MessageDTO {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private Instant createdAt;
    private Instant readAt;
}
