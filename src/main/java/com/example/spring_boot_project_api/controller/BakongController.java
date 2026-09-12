package com.example.spring_boot_project_api.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.bakong.BakongRequest;
import com.example.spring_boot_project_api.dto.request.bakong.CheckTransactionRequest;
import com.example.spring_boot_project_api.dto.response.bakong.BakongResponse;
import com.example.spring_boot_project_api.service.BakongService;

import jakarta.validation.Valid;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

@RestController
@RequestMapping("/api/v1/bakong")
public class BakongController {

    private final BakongService bakongService;

    public BakongController(BakongService bakongService) {
        this.bakongService = bakongService;
    }

    @PostMapping("/generate-qr")
    public KHQRResponse<KHQRData> generateQR(@RequestBody BakongRequest request) {
        return bakongService.generateQR(request);
    }

    @PostMapping("/get-qr-image")
    public ResponseEntity<byte[]> getQRImage(@RequestBody KHQRData qr) {
        byte[] imageBytes = bakongService.getQRImage(qr);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"qrcode.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }

    @PostMapping("/check-transaction")
    public ResponseEntity<BakongResponse> checkTransaction(
            @Valid @RequestBody CheckTransactionRequest request) {
        return ResponseEntity.ok(bakongService.checkTransactionByMD5(request));
    }
}