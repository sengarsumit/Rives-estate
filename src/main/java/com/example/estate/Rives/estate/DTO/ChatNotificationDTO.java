package com.example.estate.Rives.estate.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

// Pushed to the recipient's personal /user/queue/notifications on every new
// message, so the inbox unread badge can update live even outside the open
// thread. Purely a live-delta on top of GET /conversations, which remains
// the authoritative unread count on page load.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatNotificationDTO {
    private UUID conversationId;
    private String propertyTitle;
    private String preview;
    private Instant sentAt;
}
