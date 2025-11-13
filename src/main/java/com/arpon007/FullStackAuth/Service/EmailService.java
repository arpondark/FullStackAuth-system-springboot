package com.arpon007.FullStackAuth.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private MimeMessageHelper createMessage(String toEmail, String subject) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        return helper;
    }

    public void sendWelcomeEmail(String toEmail, String name) {
        try {
            MimeMessageHelper helper = createMessage(toEmail, "Welcome to Our Platform");
            String html = EmailTemplates.welcomeTemplate(name);
            helper.setText(html, true);
            mailSender.send(helper.getMimeMessage());
            log.info("Welcome email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error sending welcome email: {}", e.getMessage());
        }
    }

    public void sendResetOtpEmail(String toEmail, String otp) {
        try {
            MimeMessageHelper helper = createMessage(toEmail, "Password Reset OTP");
            String html = EmailTemplates.resetOtpTemplate(otp);
            helper.setText(html, true);
            mailSender.send(helper.getMimeMessage());
            log.info("Reset OTP email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error sending reset OTP email: {}", e.getMessage());
        }
    }

    public void sendVerificationOtpEmail(String toEmail, String otp) {
        try {
            MimeMessageHelper helper = createMessage(toEmail, "Verification OTP");
            String html = EmailTemplates.verificationOtpTemplate(otp);
            helper.setText(html, true);
            mailSender.send(helper.getMimeMessage());
            log.info("Verification OTP email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error sending verification OTP email: {}", e.getMessage());
        }
    }
}
