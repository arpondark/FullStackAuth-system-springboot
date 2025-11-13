package com.arpon007.FullStackAuth.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail, String name) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Welcome to Out Platform");
        message.setText("Hello " + name + ",\n\nWelcome to Our Platfrom.\n\nRegards,\nDrac Team");
        mailSender.send(message);
        log.info("Email sent to {}", toEmail);
    }

    public void sendResetOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset OTP");
        message.setText("Hello,\n\nYour OTP for password reset is: " + otp + "\n\nThis OTP is valid for 15 minutes.\n\nRegards,\nDrac Team");
        mailSender.send(message);
        log.info("Password reset OTP email sent to {}", toEmail + " with OTP: " + otp);
    }

}
