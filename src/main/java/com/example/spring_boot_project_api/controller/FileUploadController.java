package com.example.spring_boot_project_api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.FileUploadResponse;
import com.example.spring_boot_project_api.service.UploadStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {
    private final UploadStorageService uploadStorageService;

    public FileUploadController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @Operation(summary = "Upload an image to Cloudflare R2")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image uploaded successfully")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {
        return ResponseEntity.ok(uploadStorageService.uploadImage(file, folder));
    }
}