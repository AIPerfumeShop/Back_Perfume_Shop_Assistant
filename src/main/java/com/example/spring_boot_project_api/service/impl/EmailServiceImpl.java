package com.example.spring_boot_project_api.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.spring_boot_project_api.enums.OtpPurpose;
import com.example.spring_boot_project_api.service.EmailService;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.from-name:Blossom Fragrance Perfume Shop}")
    private String fromName;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtp(String toEmail, String otp, OtpPurpose purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("[" + fromName + "] " + subject(purpose));
            helper.setText(body(purpose, otp));
            mailSender.send(message);
            log.info("{} OTP sent to {} by {}", purpose, toEmail, fromEmail);
        } catch (Exception e) {
            log.warn("{} OTP [{}] for [{}] could not be emailed ({}), see app console", purpose, otp, toEmail, e.getMessage());
        }
    }

    private String subject(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTRATION -> "Your Verification Code";
            case PASSWORD_RESET -> "Your Password Reset Code";
            case EMAIL_CHANGE -> "Your Email Change Code";
        };
    }

    private String body(OtpPurpose purpose, String otp) {
        String line = switch (purpose) {
            case REGISTRATION -> "Use this code to activate your Blossom Fragrance Perfume Shop account.";
            case PASSWORD_RESET -> "Use this code to reset your Blossom Fragrance Perfume Shop password.";
            case EMAIL_CHANGE -> "Use this code to confirm the new email on your Blossom Fragrance Perfume Shop account.";
        };
        return "Your verification code is: " + otp + "\n\n" + line
                + "\n\nThis code expires in 10 minutes. If you did not request this, you can ignore this email.";
    }
}