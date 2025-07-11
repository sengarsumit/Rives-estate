package com.example.estate.Rives.estate.repository;

import com.example.estate.Rives.estate.model.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {
    List<PropertyImage> findByPropertyId(UUID propertyId);
    PropertyImage findByPublicId(String publicId);
    void deleteByPropertyId(UUID propertyId);
}
