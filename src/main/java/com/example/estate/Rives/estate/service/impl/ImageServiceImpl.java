package com.example.estate.Rives.estate.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.estate.Rives.estate.exception.ResourceNotFoundException;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.PropertyImage;
import com.example.estate.Rives.estate.repository.PropertyImageRepository;
import com.example.estate.Rives.estate.repository.PropertyRepository;
import com.example.estate.Rives.estate.service.ImageService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    static final int MAX_FILES = 10;
    static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB
    static final int MAX_IMAGES_PER_PROPERTY = 2;
    static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository imageRepository;

    // Upload multiple images for a given property ID
    @Override
    public List<PropertyImage> uploadImages(List<MultipartFile> files, UUID propertyId) {
        validateFiles(files);

        Optional<Property> optionalProperty = propertyRepository.findById(propertyId);
        if (optionalProperty.isEmpty()) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }

        Property property = optionalProperty.get();

        if (property.getImages().size() + files.size() > MAX_IMAGES_PER_PROPERTY) {
            throw new IllegalArgumentException(
                    "A property can have at most " + MAX_IMAGES_PER_PROPERTY
                            + " images (it currently has " + property.getImages().size() + ")");
        }

        List<PropertyImage> uploaded = new ArrayList<>();

        for (MultipartFile file : files) {
            String uniqueName = UUID.randomUUID().toString();
            String publicId = "properties/" + uniqueName;

            try {
                Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                        ObjectUtils.asMap(
                                "public_id", publicId,
                                "resource_type", "image"
                        ));

                String url = (String) result.get("secure_url");

                PropertyImage image = new PropertyImage();
                image.setImageUrl(url);
                image.setPublicId(publicId);
                image.setProperty(property);
                imageRepository.save(image);

                uploaded.add(image);

            } catch (IOException e) {
                throw new RuntimeException("Image upload failed: " + e.getMessage(), e);
            }
        }

        return uploaded;
    }

    // Guards the upload against empty/oversized/wrong-type files. Content type is
    // checked instead of the filename extension, which is trivial to spoof. The
    // per-file size limit here is a defensive backstop; Spring's multipart resolver
    // (spring.servlet.multipart.max-file-size) is the first line of enforcement.
    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required");
        }
        if (files.size() > MAX_FILES) {
            throw new IllegalArgumentException(
                    "A maximum of " + MAX_FILES + " images can be uploaded at once");
        }
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Image file is empty");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Each image must be 5MB or smaller");
            }
            String contentType = file.getContentType();
            if (contentType == null
                    || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
                throw new IllegalArgumentException(
                        "Unsupported image type. Allowed types: JPEG, PNG, WebP, GIF");
            }
        }
    }

    @Override
    public String getUrlFromPublicId(String publicId) {
        return cloudinary.url().secure(true).generate(publicId);
    }

    @Override
    public boolean deleteImage(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            throw new RuntimeException("Image deletion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePropertyImage(UUID propertyId, UUID imageId) {
        if (!propertyRepository.existsById(propertyId)) {
            throw new ResourceNotFoundException("Property not found with id: " + propertyId);
        }

        PropertyImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        if (!image.getProperty().getId().equals(propertyId)) {
            throw new ResourceNotFoundException("Image not found with id: " + imageId);
        }

        deleteImage(image.getPublicId());
        imageRepository.delete(image);
    }
}
