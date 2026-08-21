package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.DTO.PropertyCreateDTO;
import com.example.estate.Rives.estate.DTO.PropertyResponseDTO;
import com.example.estate.Rives.estate.DTO.PropertyUpdateDTO;
import com.example.estate.Rives.estate.DTO.config.PropertyMapper;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.service.ImageService;
import com.example.estate.Rives.estate.service.PropertyService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/properties")
public class PropertyController {

    // Whitelist of Property columns clients may sort search results by. Anything
    // outside this set is rejected so an arbitrary field name never reaches the DB.
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "rental", "locality");

    @Autowired
    UserRepository userRepository;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private PropertyMapper propertyMapper;

    @PreAuthorize("hasRole('DEALER')")
    @PostMapping("/create")
    public ResponseEntity<?> createProperty(@Valid @RequestBody PropertyCreateDTO dto, @AuthenticationPrincipal User loggedInUser) {

        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        if (propertyService.isPropertyExistByTitle(dto.getTitle())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Property already exists with this title");
        }

        Property property = new Property();
        property.setTitle(dto.getTitle());
        property.setDescription(dto.getDescription());
        property.setAddress(dto.getAddress());
        property.setLocality(dto.getLocality());
        property.setRental(dto.getRental());
        property.setDealer(loggedInUser);

        Property saved = propertyService.save(property);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasRole('DEALER')")
    @PostMapping("/{propertyId}/upload-images")
    public ResponseEntity<?> uploadImages(@PathVariable UUID propertyId,
                                          @RequestParam("images") List<MultipartFile> images,
                                          @AuthenticationPrincipal User loggedInUser) {
        Property property = propertyService.getPropertyById(propertyId);

        if (!property.getDealer().getId().equals(loggedInUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the owner dealer can upload images");
        }

        List<String> imageUrls = imageService.uploadImages(images, propertyId);
        return ResponseEntity.ok(imageUrls);
    }

    @PreAuthorize("hasAnyRole('USER','DEALER','ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Property>> getAllProperty() {
        return ResponseEntity.ok(propertyService.findAllProperties());
    }

    @PreAuthorize("hasAnyRole('USER','DEALER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Property> getPropertyById(@PathVariable UUID id) {
        return ResponseEntity.ok(propertyService.getPropertyById(id));
    }

    @PreAuthorize("hasRole('DEALER')")
    @GetMapping("/mine")
    public ResponseEntity<List<Property>> getMyProperties(@AuthenticationPrincipal User loggedInUser) {
        return ResponseEntity.ok(propertyService.getPropertiesByDealer(loggedInUser));
    }

    @PreAuthorize("hasRole('DEALER')")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteProperty(@PathVariable UUID id, @AuthenticationPrincipal User loggedInUser) {
        Property existingProperty = propertyService.getPropertyById(id);
        if (!loggedInUser.getId().equals(existingProperty.getDealer().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("only owners can delete properties");
        }
        propertyService.delete(existingProperty);
        return ResponseEntity.ok().body("property deleted");
    }

    @PreAuthorize("hasRole('DEALER')")
    @PatchMapping("/{id}")
    public ResponseEntity<?> updateProperty(@PathVariable UUID id,
                                            @Valid @RequestBody PropertyUpdateDTO updates,
                                            @AuthenticationPrincipal User loggedInUser) {
        Property existingProperty = propertyService.getPropertyById(id);
        if (!loggedInUser.getId().equals(existingProperty.getDealer().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("only owners can update properties");
        }

        if (updates.getTitle() != null) {
            existingProperty.setTitle(updates.getTitle());
        }
        if (updates.getDescription() != null) {
            existingProperty.setDescription(updates.getDescription());
        }
        if (updates.getAddress() != null) {
            existingProperty.setAddress(updates.getAddress());
        }
        if (updates.getLocality() != null) {
            existingProperty.setLocality(updates.getLocality());
        }
        if (updates.getRental() != null) {
            existingProperty.setRental(updates.getRental());
        }

        Property updated = propertyService.save(existingProperty);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAnyRole('USER','DEALER','ADMIN')")
    @GetMapping("/search/locality")
    public ResponseEntity<Page<PropertyResponseDTO>> searchByLocality(
            @RequestParam String locality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field. Allowed: " + ALLOWED_SORT_FIELDS);
        }
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Property> properties = propertyService.searchByLocality(locality, pageable);

        Page<PropertyResponseDTO> dtoPage = properties.map(propertyMapper::propertyToDto);

        return ResponseEntity.ok(dtoPage);
    }
}
