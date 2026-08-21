package com.example.estate.Rives.estate.DTO.config;

import com.example.estate.Rives.estate.DTO.PropertyResponseDTO;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.PropertyImage;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PropertyMapper {

    private final ModelMapper modelMapper;

    public PropertyResponseDTO propertyToDto(Property property) {
        PropertyResponseDTO dto = modelMapper.map(property, PropertyResponseDTO.class);
        List<PropertyImage> images = property.getImages();
        dto.setImageUrls(images == null ? Collections.emptyList() :
                images.stream().map(PropertyImage::getImageUrl).toList());
        return dto;
    }
}
