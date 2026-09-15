package com.example.spring_boot_project_api.dto.request.bakong;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QRImageRequest {
    @NotBlank(message = "QR data is required")
    private String qr;
}
