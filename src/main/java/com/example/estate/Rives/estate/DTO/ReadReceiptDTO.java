package com.example.estate.Rives.estate.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

// Broadcast to /topic/conversations/{id}/read whenever a participant marks
// the other side's messages as read.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceiptDTO {
    private UUID conversationId;
    private UUID readerId;
    private Instant readAt;
}
