package com.example.estate.Rives.estate.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.estate.Rives.estate.model.Property;
import com.example.estate.Rives.estate.model.PropertyImage;
import com.example.estate.Rives.estate.repository.PropertyImageRepository;
import com.example.estate.Rives.estate.repository.PropertyRepository;
import com.example.estate.Rives.estate.service.ImageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ImageServiceImpl implements ImageService {

    private final Cloudinary cloudinary;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyImageRepository imageRepository;

    public ImageServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // Upload multiple images for a given property ID
    @Override
    public List<String> uploadImages(List<MultipartFile> files, UUID propertyId) {
        Optional<Property> optionalProperty = propertyRepository.findById(propertyId);
        if (optionalProperty.isEmpty()) {
            throw new IllegalArgumentException("Property not found with id: " + propertyId);
        }

        Property property = optionalProperty.get();
        List<String> imageUrls = new ArrayList<>();

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

                imageUrls.add(url);

            } catch (IOException e) {
                throw new RuntimeException("Image upload failed: " + e.getMessage(), e);
            }
        }

        return imageUrls;
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
}
