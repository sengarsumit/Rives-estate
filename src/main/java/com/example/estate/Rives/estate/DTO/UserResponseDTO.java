package com.example.estate.Rives.estate.DTO;

import com.example.estate.Rives.estate.enums.Role;
import lombok.Data;

import java.util.UUID;

@Data
public class UserResponseDTO {
    private UUID id;
    private String username;
    private String email;
    private Role role;
}
