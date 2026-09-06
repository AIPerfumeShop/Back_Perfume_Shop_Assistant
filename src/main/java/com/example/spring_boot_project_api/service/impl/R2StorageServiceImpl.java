package com.example.spring_boot_project_api.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring_boot_project_api.dto.response.FileUploadResponse;
import com.example.spring_boot_project_api.exception.UploadStorageException;
import com.example.spring_boot_project_api.service.UploadStorageService;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class R2StorageServiceImpl implements UploadStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_FOLDERS = Set.of(
            "products", "brands", "categories");
    private static final String DEFAULT_FOLDER = "products";

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicUrl;

    public R2StorageServiceImpl(
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.access-key-id}") String accessKeyId,
            @Value("${r2.secret-access-key}") String secretAccessKey,
            @Value("${r2.bucket-name}") String bucketName,
            @Value("${r2.public-url}") String publicUrl) {

        this.bucketName = bucketName;
        this.publicUrl = publicUrl;

        this.s3Client = S3Client.builder()
                .region(Region.of("auto"))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .forcePathStyle(true)
                .build();
    }

    @Override
    public FileUploadResponse uploadImage(MultipartFile file) {
        return uploadImage(file, DEFAULT_FOLDER);
    }

    @Override
    public FileUploadResponse uploadImage(MultipartFile file, String folder) {
        validate(file);

        String folderPath = validateFolder(folder);
        String contentType = file.getContentType();
        String extension = getExtension(file.getOriginalFilename(), contentType);
        String baseName = getBaseName(file.getOriginalFilename());
        String key = folderPath + "/" + UUID.randomUUID() + "-"
                + baseName + extension;

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException ex) {
            throw new UploadStorageException("Failed to read uploaded file", ex);
        }

        return new FileUploadResponse(
                publicUrl + "/" + key,
                key,
                contentType,
                file.getSize());
    }

    private String validateFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return DEFAULT_FOLDER;
        }
        if (!ALLOWED_FOLDERS.contains(folder)) {
            throw new UploadStorageException(
                    "Unsupported folder: " + folder + ". Allowed: products, brands, categories");
        }
        return folder;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadStorageException("Uploaded file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new UploadStorageException(
                    "Unsupported file type: " + contentType + ". Allowed: jpeg, png, webp");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new UploadStorageException(
                    "File too large. Maximum allowed size is " + (MAX_FILE_SIZE / 1024 / 1024) + " MB");
        }
    }

    private String getExtension(String filename, String contentType) {
        if (filename != null) {
            String extension = StringUtils.getFilenameExtension(filename);
            if (extension != null) {
                return "." + extension.toLowerCase();
            }
        }
        if (contentType != null) {
            return switch (contentType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> "";
            };
        }
        return "";
    }

    private String getBaseName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image";
        }
        String baseName = filename;
        String extension = StringUtils.getFilenameExtension(filename);
        if (extension != null) {
            baseName = baseName.substring(0, baseName.length() - extension.length() - 1);
        }
        return baseName;
    }
}