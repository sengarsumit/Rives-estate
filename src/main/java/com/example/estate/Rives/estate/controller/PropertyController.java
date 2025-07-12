package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.service.ImageService;
import com.example.estate.Rives.estate.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/properties")
public class PropertyController {

    @Autowired
    UserRepository userRepository;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private ImageService imageService;

    @PreAuthorize("hasRole('DEALER')")
    @PostMapping("/create")
    public ResponseEntity<?> createProperty(@RequestBody Property property, @AuthenticationPrincipal User loggedInUser) {

        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        if (propertyService.isPropertyExistByTitle(property.getTitle())) {
            return ResponseEntity.badRequest().body("Property already exists with this title");
        }

        property.setDealer(loggedInUser);
        Property saved = propertyService.save(property);
        return ResponseEntity.ok(saved);
    }

    @PreAuthorize("hasRole('DEALER')")
    @PostMapping("/{propertyId}/upload-images")
    public ResponseEntity<?> uploadImages(@PathVariable UUID propertyId,
                                          @RequestParam("images") List<MultipartFile> images,
                                          @AuthenticationPrincipal User loggedInUser) {
        Property property = propertyService.getPropertyById(propertyId);
        if (property == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Property not found");
        }

        if (!property.getDealer().getId().equals(loggedInUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the owner dealer can upload images");
        }

        List<String> imageUrls = imageService.uploadImages(images, propertyId);
        return ResponseEntity.ok(imageUrls);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/all")
    public ResponseEntity<List<Property>> getAllProperty() {
        return ResponseEntity.ok(propertyService.findAllProperties());
    }

    @PreAuthorize("hasRole('DEALER')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProperty(@PathVariable UUID id, @AuthenticationPrincipal User loggedInUser) {
        Property existingProperty = propertyService.getPropertyById(id);
        if (propertyService.isPropertyExist(id) && loggedInUser.getId().equals(existingProperty.getDealer().getId())) {
            propertyService.delete(existingProperty);
            return ResponseEntity.ok().body("property deleted");
        }
        return ResponseEntity.badRequest().body("property not found");
    }

    @PreAuthorize("hasRole('DEALER')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateProperty(@PathVariable UUID id,
                                            @RequestBody Property propertyUpdates,
                                            @AuthenticationPrincipal User loggedInUser) {
        if (!propertyService.isPropertyExist(id)) {
            return ResponseEntity.badRequest().body("property not found");
        }

        Property existingProperty = propertyService.getPropertyById(id);
        if (!loggedInUser.getId().equals(existingProperty.getDealer().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("only owners can update properties");
        }

        if (propertyUpdates.getTitle() != null) {
            existingProperty.setTitle(propertyUpdates.getTitle());
        }
        if (propertyUpdates.getDescription() != null) {
            existingProperty.setDescription(propertyUpdates.getDescription());
        }
        if (propertyUpdates.getAddress() != null) {
            existingProperty.setAddress(propertyUpdates.getAddress());
        }
        if (propertyUpdates.getLocality() != null) {
            existingProperty.setLocality(propertyUpdates.getLocality());
        }
        if (propertyUpdates.getRental() != null) {
            existingProperty.setRental(propertyUpdates.getRental());
        }

        Property updated = propertyService.save(existingProperty);
        return ResponseEntity.ok(updated);
    }
}
