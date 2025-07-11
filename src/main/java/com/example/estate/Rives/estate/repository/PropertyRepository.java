package com.example.estate.Rives.estate.repository;

import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PropertyRepository extends JpaRepository<Property, UUID> {
    Optional<Property> findByTitle(String title);
    List<Property> findByLocalityContainingIgnoreCase(String locality);
    List<Property> findByDealer(User dealer);
    boolean existsByTitle(String title);
    Optional<Property> findById(UUID id);
}

