package com.example.estate.Rives.estate.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.example.estate.Rives.estate.exception.ResourceNotFoundException;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.PropertyImage;
import com.example.estate.Rives.estate.repository.PropertyImageRepository;
import com.example.estate.Rives.estate.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private PropertyImageRepository imageRepository;

    @InjectMocks
    private ImageServiceImpl imageService;

    private static MockMultipartFile jpeg(byte[] content) {
        return new MockMultipartFile("images", "photo.jpg", "image/jpeg", content);
    }

    @Test
    void uploadImages_noFiles_isRejectedBeforeAnyLookupOrUpload() {
        assertThatThrownBy(() -> imageService.uploadImages(List.of(), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one image");

        verifyNoInteractions(propertyRepository, cloudinary, imageRepository);
    }

    @Test
    void uploadImages_tooManyFiles_isRejected() {
        List<MultipartFile> files = IntStream.range(0, ImageServiceImpl.MAX_FILES + 1)
                .mapToObj(i -> (MultipartFile) jpeg(new byte[] {1, 2, 3}))
                .collect(Collectors.toList());

        assertThatThrownBy(() -> imageService.uploadImages(files, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void uploadImages_emptyFile_isRejected() {
        assertThatThrownBy(() ->
                imageService.uploadImages(List.of(jpeg(new byte[0])), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void uploadImages_oversizedFile_isRejected() {
        byte[] tooBig = new byte[(int) ImageServiceImpl.MAX_FILE_SIZE + 1];
        assertThatThrownBy(() ->
                imageService.uploadImages(List.of(jpeg(tooBig)), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void uploadImages_disallowedContentType_isRejected() {
        MockMultipartFile pdf =
                new MockMultipartFile("images", "doc.pdf", "application/pdf", new byte[] {1, 2, 3});

        assertThatThrownBy(() ->
                imageService.uploadImages(List.of(pdf), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image type");
    }

    @Test
    void uploadImages_nullContentType_isRejected() {
        MockMultipartFile noType =
                new MockMultipartFile("images", "photo", null, new byte[] {1, 2, 3});

        assertThatThrownBy(() ->
                imageService.uploadImages(List.of(noType), UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image type");
    }

    @Test
    void uploadImages_validFiles_passValidationThenHitPropertyLookup() {
        UUID id = UUID.randomUUID();
        when(propertyRepository.findById(id)).thenReturn(Optional.empty());

        // A valid image clears validation, so execution reaches the property
        // lookup (which we stub as missing) rather than being rejected as input.
        assertThatThrownBy(() ->
                imageService.uploadImages(List.of(jpeg(new byte[] {1, 2, 3})), id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Property propertyWithImages(int imageCount) {
        Property property = new Property();
        property.setId(UUID.randomUUID());
        List<PropertyImage> images = new ArrayList<>();
        for (int i = 0; i < imageCount; i++) {
            PropertyImage image = new PropertyImage();
            image.setId(UUID.randomUUID());
            image.setPublicId("properties/" + UUID.randomUUID());
            image.setProperty(property);
            images.add(image);
        }
        property.setImages(images);
        return property;
    }

    @Test
    void uploadImages_freshPropertyTooManyFilesForCap_isRejected() {
        UUID id = UUID.randomUUID();
        when(propertyRepository.findById(id)).thenReturn(Optional.of(propertyWithImages(0)));

        List<MultipartFile> files = List.of(
                jpeg(new byte[] {1}), jpeg(new byte[] {2}), jpeg(new byte[] {3}));

        assertThatThrownBy(() -> imageService.uploadImages(files, id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 2 images");

        verifyNoInteractions(cloudinary, imageRepository);
    }

    @Test
    void uploadImages_atCap_rejectsAdditionalUpload() {
        UUID id = UUID.randomUUID();
        when(propertyRepository.findById(id)).thenReturn(Optional.of(propertyWithImages(2)));

        assertThatThrownBy(() ->
                imageService.uploadImages(List.of(jpeg(new byte[] {1})), id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 2 images");

        verifyNoInteractions(cloudinary, imageRepository);
    }

    @Test
    void uploadImages_withinCap_succeedsAndReturnsImages() throws Exception {
        UUID id = UUID.randomUUID();
        when(propertyRepository.findById(id)).thenReturn(Optional.of(propertyWithImages(0)));

        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap()))
                .thenReturn(Map.of("secure_url", "https://cloudinary.example/photo.jpg"));

        List<MultipartFile> files = List.of(jpeg(new byte[] {1, 2, 3}), jpeg(new byte[] {4, 5, 6}));

        List<PropertyImage> result = imageService.uploadImages(files, id);

        assertThat(result).hasSize(2);
        assertThat(result)
                .allSatisfy(img -> assertThat(img.getImageUrl()).isEqualTo("https://cloudinary.example/photo.jpg"));
        verify(imageRepository, times(2)).save(any(PropertyImage.class));
    }

    @Test
    void deletePropertyImage_unknownProperty_throwsNotFound() {
        UUID propertyId = UUID.randomUUID();
        when(propertyRepository.existsById(propertyId)).thenReturn(false);

        assertThatThrownBy(() -> imageService.deletePropertyImage(propertyId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(imageRepository, cloudinary);
    }

    @Test
    void deletePropertyImage_unknownImage_throwsNotFound() {
        UUID propertyId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        when(propertyRepository.existsById(propertyId)).thenReturn(true);
        when(imageRepository.findById(imageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageService.deletePropertyImage(propertyId, imageId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletePropertyImage_belongsToDifferentProperty_throwsNotFound() {
        UUID propertyId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        Property otherProperty = new Property();
        otherProperty.setId(UUID.randomUUID());

        PropertyImage image = new PropertyImage();
        image.setId(imageId);
        image.setProperty(otherProperty);

        when(propertyRepository.existsById(propertyId)).thenReturn(true);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> imageService.deletePropertyImage(propertyId, imageId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(imageRepository, never()).delete(any());
    }

    @Test
    void deletePropertyImage_success_deletesFromCloudinaryAndRepository() throws Exception {
        UUID propertyId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        Property property = new Property();
        property.setId(propertyId);

        PropertyImage image = new PropertyImage();
        image.setId(imageId);
        image.setPublicId("properties/abc123");
        image.setProperty(property);

        when(propertyRepository.existsById(propertyId)).thenReturn(true);
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("properties/abc123"), anyMap())).thenReturn(Map.of("result", "ok"));

        imageService.deletePropertyImage(propertyId, imageId);

        verify(uploader).destroy(eq("properties/abc123"), anyMap());
        verify(imageRepository).delete(image);
    }
}
