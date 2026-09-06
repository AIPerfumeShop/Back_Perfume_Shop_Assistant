package com.example.spring_boot_project_api.service;

import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.FileUploadResponse;

public interface UploadStorageService {
    FileUploadResponse uploadImage(MultipartFile file);

    FileUploadResponse uploadImage(MultipartFile file, String folder);
}