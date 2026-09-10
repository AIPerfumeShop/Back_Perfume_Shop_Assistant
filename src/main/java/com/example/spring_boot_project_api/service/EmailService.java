package com.example.spring_boot_project_api.service;

import com.example.spring_boot_project_api.enums.OtpPurpose;

public interface EmailService {

    void sendOtp(String toEmail, String otp, OtpPurpose purpose);
}