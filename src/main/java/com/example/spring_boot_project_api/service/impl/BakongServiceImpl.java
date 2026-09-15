package com.example.spring_boot_project_api.service.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.spring_boot_project_api.config.BakongProperties;
import com.example.spring_boot_project_api.dto.request.bakong.BakongRequest;
import com.example.spring_boot_project_api.dto.request.bakong.CheckTransactionRequest;
import com.example.spring_boot_project_api.dto.response.bakong.BakongResponse;
import com.example.spring_boot_project_api.exception.BakongException;
import com.example.spring_boot_project_api.service.BakongService;
import com.example.spring_boot_project_api.service.BakongTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

@Service
public class BakongServiceImpl implements BakongService {

    private static final Logger log = LoggerFactory.getLogger(BakongServiceImpl.class);

    private final BakongProperties properties;
    private final BakongTokenService bakongTokenService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BakongServiceImpl(BakongProperties properties,
                             BakongTokenService bakongTokenService) {
        this.properties = properties;
        this.bakongTokenService = bakongTokenService;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public KHQRResponse<KHQRData> generateQR(BakongRequest request) {
        IndividualInfo individualInfo = new IndividualInfo();

        individualInfo.setExpirationTimestamp(
                System.currentTimeMillis()
                        + request.expirationTimestamp() * 60 * 1000);

        individualInfo.setBakongAccountId(properties.getAccountId());
        individualInfo.setCurrency(request.currency());
        individualInfo.setAmount(request.amount());
        individualInfo.setMerchantName(request.merchantName());
        individualInfo.setMerchantCity(request.merchantCity());
        individualInfo.setBillNumber(request.billNumber());
        individualInfo.setMobileNumber(request.mobileNumber());
        individualInfo.setStoreLabel(request.storeLabel());
        individualInfo.setUpiAccountInformation(request.upiAccountInformation());
        individualInfo.setMerchantAlternateLanguagePreference(
                request.merchantAlternateLanguagePreference());
        individualInfo.setMerchantNameAlternateLanguage(
                request.merchantNameAlternateLanguage());
        individualInfo.setMerchantCityAlternateLanguage(
                request.merchantCityAlternateLanguage());
        individualInfo.setPurposeOfTransaction(request.purposeOfTransaction());
        individualInfo.setTerminalLabel(request.terminalLabel());

        return BakongKHQR.generateIndividual(individualInfo);
    }

    @Override
    public byte[] getQRImage(KHQRData qr) {
        try {
            if (qr == null || qr.getQr() == null || qr.getQr().isBlank()) {
                return "Invalid QR data".getBytes(StandardCharsets.UTF_8);
            }

            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(
                    qr.getQr(), BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();
        } catch (WriterException ex) {
            return "Error encoding QR data".getBytes(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return ("Unexpected error: " + ex.getMessage())
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public BakongResponse checkTransactionByMD5(CheckTransactionRequest request) {
        if (!properties.isConfigured()) {
            throw new BakongException(
                    "Bakong is not configured. Set BAKONG_ACCOUNT_ID, "
                            + "BAKONG_BASE_URL and EMAIL in .env");
        }
        String bearerToken = bakongTokenService.getAccessToken();

        String responseBody = restClient.post()
                .uri("/v1/check_transaction_by_md5")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(Map.of("md5", request.md5()))
                .retrieve()
                .body(String.class);

        log.info("Response from Bakong API: {}", responseBody);

        try {
            return objectMapper.readValue(responseBody, BakongResponse.class);
        } catch (Exception ex) {
            throw new BakongException("Invalid upstream response", ex);
        }
    }
}