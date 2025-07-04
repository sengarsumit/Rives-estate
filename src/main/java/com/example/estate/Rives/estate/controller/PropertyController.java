package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.User;
import com.example.estate.Rives.estate.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/properties")
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @PostMapping("/create")
    public ResponseEntity<?> createProperty(@RequestBody Property property){
        if(propertyService.isPropertyExist(property.getTitle())){
            return ResponseEntity.badRequest().body("property already exist");
        }
        Property property1=propertyService.save(property);
        return ResponseEntity.ok(property1);
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
    public ResponseEntity<?> updateProperty(@PathVariable String title,@RequestBody Property propertyUpdates){
        if(!propertyService.isPropertyExist(title)){
            return ResponseEntity.badRequest().body("property not found");
        }
        Property existingProperty=propertyService.getPropertyByTitle(title);

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
