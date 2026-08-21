package com.example.estate.Rives.estate.DTO.config;

import com.example.estate.Rives.estate.DTO.PropertyResponseDTO;
import com.example.estate.Rives.estate.config.ModelMapperConfig;
import com.example.estate.Rives.estate.enums.Role;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.PropertyImage;
import com.example.estate.Rives.estate.model.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyMapperTest {

    private final PropertyMapper propertyMapper = new PropertyMapper(new ModelMapperConfig().modelMapper());

    @Test
    void propertyToDto_mapsImageUrlsFromPropertyImages() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setTitle("Sea View Villa");
        property.setDescription("A nice villa");
        property.setAddress("123 Beach Rd");
        property.setLocality("Goa");
        property.setRental(50000.0);

        User dealer = new User();
        dealer.setId(UUID.randomUUID());
        dealer.setUsername("dealerbob");
        dealer.setEmail("bob@dealers.com");
        dealer.setRole(Role.DEALER);
        property.setDealer(dealer);

        PropertyImage image1 = new PropertyImage();
        image1.setId(UUID.randomUUID());
        image1.setImageUrl("https://cdn.example.com/1.jpg");
        image1.setPublicId("pub1");

        PropertyImage image2 = new PropertyImage();
        image2.setId(UUID.randomUUID());
        image2.setImageUrl("https://cdn.example.com/2.jpg");
        image2.setPublicId("pub2");

        property.setImages(List.of(image1, image2));

        PropertyResponseDTO dto = propertyMapper.propertyToDto(property);

        assertThat(dto.getTitle()).isEqualTo("Sea View Villa");
        assertThat(dto.getRental()).isEqualTo(50000.0);
        assertThat(dto.getDealer()).isNotNull();
        assertThat(dto.getDealer().getUsername()).isEqualTo("dealerbob");
        assertThat(dto.getImageUrls())
                .containsExactly("https://cdn.example.com/1.jpg", "https://cdn.example.com/2.jpg");
    }

    @Test
    void propertyToDto_withNoImages_returnsEmptyList() {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        property.setTitle("Empty Listing");
        property.setAddress("Nowhere");

        PropertyResponseDTO dto = propertyMapper.propertyToDto(property);

        assertThat(dto.getImageUrls()).isEmpty();
    }
}
