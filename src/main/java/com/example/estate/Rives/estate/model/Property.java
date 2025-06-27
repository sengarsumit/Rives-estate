package com.example.estate.Rives.estate.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
@Entity
public class Property {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String title;

    private String description;
    @Column(nullable = false)
    private String address;

    private String locality;
    private Double rental;

    @ManyToOne
    @JoinColumn(name = "dealer_id")
    private User dealer;
}
