package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.dto.request.bakong.BakongRequest;
import com.example.spring_boot_project_api.dto.request.bakong.CheckTransactionRequest;
import com.example.spring_boot_project_api.dto.response.bakong.BakongResponse;

import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

public interface BakongService {

    KHQRResponse<KHQRData> generateQR(BakongRequest request);

    byte[] getQRImage(KHQRData qr);

    BakongResponse checkTransactionByMD5(CheckTransactionRequest request);
}