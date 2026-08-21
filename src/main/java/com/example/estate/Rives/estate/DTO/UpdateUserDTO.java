package com.example.estate.Rives.estate.DTO;

import com.example.estate.Rives.estate.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserDTO {
    private String firstName;
    private String lastName;

    @Email(message = "email must be a valid email address")
    private String email;

    private String phone;

    @Size(min = 8, message = "password must be at least 8 characters long")
    private String password;

    private Role role;
}
