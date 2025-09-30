package com.example.estate.Rives.estate.DTO;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PropertyResponseDTO {
    private UUID id;
    private String title;
    private String description;
    private String address;
    private String locality;
    private Double rental;

    private DealerDTO dealer;

    private List<String> imageUrls;

    @Data
    public static class DealerDTO {
        private UUID id;
        private String username;
        private String email;
    }
}
