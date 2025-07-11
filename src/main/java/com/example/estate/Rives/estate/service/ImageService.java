package com.example.estate.Rives.estate.service;


import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ImageService {
    List<String> uploadImages(List<MultipartFile> files, UUID propertyId);
    String getUrlFromPublicId(String publicId);
    boolean deleteImage(String publicId);
}
