package com.example.estate.Rives.estate.DTO;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class PropertyUpdateDTO {

    private String title;
    private String description;
    private String address;
    private String locality;

    @PositiveOrZero(message = "rental must not be negative")
    private Double rental;
}
