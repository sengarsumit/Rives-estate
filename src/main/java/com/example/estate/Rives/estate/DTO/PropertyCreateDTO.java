package com.example.estate.Rives.estate.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class PropertyCreateDTO {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotBlank(message = "address is required")
    private String address;

    private String locality;

    @PositiveOrZero(message = "rental must not be negative")
    private Double rental;
}
