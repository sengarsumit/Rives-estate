package com.example.estate.Rives.estate.DTO;

import com.example.estate.Rives.estate.enums.Role;
import lombok.Data;

@Data
public class UserRegisterDTO {
    private String username;
    private String email;
    private String password;
    private Role role;
}
