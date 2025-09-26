package com.example.estate.Rives.estate.service;

import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import org.hibernate.query.Page;

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

}
