package com.example.estate.Rives.estate.DTO;

import lombok.Data;

// STOMP inbound payload for /app/conversations/{id}/send. Content is
// validated in ChatServiceImpl rather than via @Valid here, since bean
// validation isn't wired into the STOMP message-handling path the way it is
// for @RequestBody on REST controllers.
@Data
public class ChatMessageRequest {
    private String content;
}
