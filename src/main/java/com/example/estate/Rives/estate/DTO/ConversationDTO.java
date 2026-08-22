package com.example.estate.Rives.estate.DTO;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ConversationDTO {
    private UUID id;
    private UUID propertyId;
    private String propertyTitle;
    private UserSummaryDTO buyer;
    private UserSummaryDTO dealer;
    private Instant createdAt;
}
