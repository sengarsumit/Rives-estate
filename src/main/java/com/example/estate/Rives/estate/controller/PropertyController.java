package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.repository.UserRepository;
import com.example.estate.Rives.estate.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/properties")
public class PropertyController {

    @Autowired
    UserRepository userRepository;
    @Autowired
    private PropertyService propertyService;

    @PostMapping("/create")
    public ResponseEntity<?> createProperty(@RequestBody Property property,@AuthenticationPrincipal User loggedInUser){
        if(!loggedInUser.getRole().equals(Role.DEALER))
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("only dealers can post properties");
       }

        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        if (propertyService.isPropertyExist(property.getTitle())) {
            return ResponseEntity.badRequest().body("Property already exists with this title");
        }

        property.setDealer(loggedInUser);
        Property saved = propertyService.save(property);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Property>> getAllProperty(){
        return ResponseEntity.ok(propertyService.findAllProperties());
    }

    @DeleteMapping("/delete/{title}")
    public ResponseEntity<?> deleteProperty(@PathVariable String title){
        if(propertyService.isPropertyExist(title)){
            Property property=propertyService.getPropertyByTitle(title);
            propertyService.delete(property);
            return ResponseEntity.ok().body("property deleted");
        }
        return ResponseEntity.badRequest().body("property not found");
    }
    @PatchMapping("/{title}")
    public ResponseEntity<?> updateProperty(@PathVariable String title,@RequestBody Property propertyUpdates,@AuthenticationPrincipal User loggedInUser){


        if(!propertyService.isPropertyExist(title)){
            return ResponseEntity.badRequest().body("property not found");
        }
        Property existingProperty=propertyService.getPropertyByTitle(title);
        if(!loggedInUser.getId().equals(existingProperty.getDealer().getId()))
        {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("only owners can update properties");
        }

        if(propertyUpdates.getTitle()!=null){
            existingProperty.setTitle(propertyUpdates.getTitle());
        }
        if(propertyUpdates.getDescription()!=null){
            existingProperty.setDescription(propertyUpdates.getDescription());
        }
        if(propertyUpdates.getAddress() != null){
            existingProperty.setAddress(propertyUpdates.getAddress());
        }
        if(propertyUpdates.getLocality()!=null){
            existingProperty.setLocality(propertyUpdates.getLocality());
        }
        if(propertyUpdates.getRental()!=null){
            existingProperty.setRental(propertyUpdates.getRental());
        }
        Property updated=propertyService.save(existingProperty);
        return ResponseEntity.ok(updated);
    }


}
