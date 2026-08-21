package com.example.estate.Rives.estate.service.impl;

import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.PropertyImage;
import com.example.estate.Rives.estate.repository.PropertyRepository;
import com.example.estate.Rives.estate.service.ImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyServiceImplTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private ImageService imageService;

    @Test
    void delete_removesEachImageFromCloudinaryBeforeDeletingProperty() {
        PropertyServiceImpl service = new PropertyServiceImpl(propertyRepository, imageService);

        PropertyImage image1 = new PropertyImage();
        image1.setPublicId("properties/pub1");
        PropertyImage image2 = new PropertyImage();
        image2.setPublicId("properties/pub2");

        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setImages(List.of(image1, image2));

        when(imageService.deleteImage("properties/pub1")).thenReturn(true);
        when(imageService.deleteImage("properties/pub2")).thenReturn(true);

        service.delete(property);

        verify(imageService).deleteImage("properties/pub1");
        verify(imageService).deleteImage("properties/pub2");
        verify(propertyRepository).delete(property);
    }

    @Test
    void delete_withNoImages_stillDeletesProperty() {
        PropertyServiceImpl service = new PropertyServiceImpl(propertyRepository, imageService);

        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setImages(List.of());

        service.delete(property);

        verify(propertyRepository).delete(property);
    }
}
