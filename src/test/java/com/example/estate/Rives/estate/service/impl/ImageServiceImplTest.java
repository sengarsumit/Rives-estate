package com.example.estate.Rives.estate.service.impl;

import com.cloudinary.Cloudinary;
import com.example.estate.Rives.estate.exception.ResourceNotFoundException;
import com.example.estate.Rives.estate.repository.PropertyImageRepository;
import com.example.estate.Rives.estate.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
