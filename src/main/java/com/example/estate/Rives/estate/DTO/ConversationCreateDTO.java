package com.example.estate.Rives.estate.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ConversationCreateDTO {
    @NotNull(message = "propertyId is required")
    private UUID propertyId;
}
