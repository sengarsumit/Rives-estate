package com.example.estate.Rives.estate.controller;

import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
