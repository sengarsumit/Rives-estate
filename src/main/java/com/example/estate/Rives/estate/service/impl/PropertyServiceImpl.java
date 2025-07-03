package com.example.estate.Rives.estate.service.impl;

import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.PropertyRepository;
import com.example.estate.Rives.estate.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PropertyServiceImpl implements PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

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
    public boolean isPropertyExist(String title) {
        return propertyRepository.existsByTitle(title);
    }
}
