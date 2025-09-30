package com.example.estate.Rives.estate.service;

import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyService {
    Optional<Property> findByTitle(String title);
    List<Property> findByLocalityContainingIgnoreCase(String locality);
    Property save(Property property);
    void delete(Property property);
    List<Property> findAllProperties();

    void updateProperty(Property property);

    List<Property> getPropertiesByDealer(User dealer);
    boolean isPropertyExist(UUID id);
    boolean isPropertyExistByTitle(String title);

    Property getPropertyByTitle(String title);

    Property getPropertyById(UUID id);
    Page<Property> searchByLocality(String locality, Pageable pageable);
}
