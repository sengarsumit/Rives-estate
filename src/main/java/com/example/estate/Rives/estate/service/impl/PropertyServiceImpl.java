package com.example.estate.Rives.estate.service.impl;

import com.example.estate.Rives.estate.exception.ResourceNotFoundException;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.PropertyImage;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.PropertyRepository;
import com.example.estate.Rives.estate.service.ImageService;
import com.example.estate.Rives.estate.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final ImageService imageService;

    @Override
    public Optional<Property> findByTitle(String title) {
        return propertyRepository.findByTitle(title);
    }

    @Override
    public List<Property> findByLocalityContainingIgnoreCase(String locality) {
        return propertyRepository.findByLocalityContainingIgnoreCase(locality);
    }

    @Override
    public Property save(Property property) {
        return propertyRepository.save(property);
    }

    @Override
    public void delete(Property property) {
        for (PropertyImage image : property.getImages()) {
            imageService.deleteImage(image.getPublicId());
        }
        propertyRepository.delete(property);
    }

    @Override
    public List<Property> findAllProperties() {
        return propertyRepository.findAll();
    }

    @Override
    public void updateProperty(Property property) {
        propertyRepository.save(property);
    }

    @Override
    public List<Property> getPropertiesByDealer(User dealer) {
        return propertyRepository.findByDealer(dealer);
    }

    @Override
    public boolean isPropertyExist(UUID id) {
        return propertyRepository.existsById(id);
    }

    @Override
    public boolean isPropertyExistByTitle(String title) {
        return propertyRepository.existsByTitle(title);
    }

    @Override
    public Property getPropertyByTitle(String title) {
        return propertyRepository.findByTitle(title)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with title: " + title));
    }

    @Override
    public Property getPropertyById(UUID id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }

    @Override
    public Page<Property> searchByLocality(String locality, Pageable pageable) {
        if (locality == null || locality.trim().isEmpty()) {
            return propertyRepository.findAll(pageable);
        }
        return propertyRepository.findByLocalityContainingIgnoreCase(locality.trim(), pageable);
    }
}
