package com.example.estate.Rives.estate.service;


import com.example.estate.Rives.estate.model.PropertyImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ImageService {
    List<PropertyImage> uploadImages(List<MultipartFile> files, UUID propertyId);
    String getUrlFromPublicId(String publicId);
    boolean deleteImage(String publicId);
    void deletePropertyImage(UUID propertyId, UUID imageId);
}
