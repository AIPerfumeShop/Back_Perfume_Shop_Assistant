package com.example.spring_boot_project_api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileUploadResponse {
    private String url;
    private String filename;
    private String contentType;
    private long size;
}