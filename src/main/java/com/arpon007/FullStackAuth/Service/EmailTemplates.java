package com.arpon007.FullStackAuth.Service;

public class EmailTemplates {

    public static String welcomeTemplate(String name) {
        return "<div style=\"max-width:600px;margin:auto;padding:20px;" +
                "font-family:Arial, sans-serif;border-radius:10px;border:1px solid #e0e0e0;\">" +
                "<h2 style=\"color:#4CAF50;text-align:center;\">Welcome to Our Platform</h2>" +
                "<p style=\"font-size:16px;\">Hello <b>" + name + "</b>,</p>" +
                "<p style=\"font-size:16px;\">" +
                "We're excited to have you with us. Explore all features and enjoy your journey." +
                "</p>" +
                "<div style=\"text-align:center;margin-top:30px;\">" +
                "<a href=\"#\" style=\"" +
                "padding:12px 25px;" +
                "background:#4CAF50;" +
                "color:white;" +
                "text-decoration:none;" +
                "border-radius:5px;" +
                "font-size:16px;" +
                "\">Get Started</a>" +
                "</div>" +
                "<p style=\"margin-top:40px;font-size:14px;color:gray;\">Regards,<br>Drac Team</p>" +
                "</div>";
    }

    public static String resetOtpTemplate(String otp) {
        return "<div style=\"max-width:600px;margin:auto;padding:20px;" +
                "font-family:Arial, sans-serif;border-radius:10px;border:1px solid #e0e0e0;\">" +
                "<h2 style=\"color:#d9534f;text-align:center;\">Password Reset OTP</h2>" +
                "<p style=\"font-size:16px;\">Hello,</p>" +
                "<p style=\"font-size:16px;\">Your OTP for password reset:</p>" +
                "<h1 style=\"text-align:center;color:#d9534f;\">" + otp + "</h1>" +
                "<p style=\"font-size:14px;margin-top:20px;\">This OTP is valid for 15 minutes.</p>" +
                "<p style=\"margin-top:40px;font-size:14px;color:gray;\">Regards,<br>Drac Team</p>" +
                "</div>";
    }

    public static String verificationOtpTemplate(String otp) {
        return "<div style=\"max-width:600px;margin:auto;padding:20px;" +
                "font-family:Arial, sans-serif;border-radius:10px;border:1px solid #e0e0e0;\">" +
                "<h2 style=\"color:#0275d8;text-align:center;\">Verification OTP</h2>" +
                "<p style=\"font-size:16px;\">Hello,</p>" +
                "<p style=\"font-size:16px;\">Your OTP for account verification:</p>" +
                "<h1 style=\"text-align:center;color:#0275d8;\">" + otp + "</h1>" +
                "<p style=\"font-size:14px;margin-top:20px;\">This OTP is valid for 15 minutes.</p>" +
                "<p style=\"margin-top:40px;font-size:14px;color:gray;\">Regards,<br>Drac Team</p>" +
                "</div>";
    }

    public static String verificationLinkTemplate(String verificationLink) {
        return "<div style=\"max-width:600px;margin:auto;padding:20px;" +
                "font-family:Arial, sans-serif;border-radius:10px;border:1px solid #e0e0e0;\">" +
                "<h2 style=\"color:#0275d8;text-align:center;\">Verify Your Email</h2>" +
                "<p style=\"font-size:16px;\">Hello,</p>" +
                "<p style=\"font-size:16px;\">Thank you for signing up! Please verify your email address by clicking the link below:</p>" +
                "<div style=\"text-align:center;margin-top:30px;\">" +
                "<a href=\"" + verificationLink + "\" style=\"" +
                "padding:12px 25px;" +
                "background:#0275d8;" +
                "color:white;" +
                "text-decoration:none;" +
                "border-radius:5px;" +
                "font-size:16px;" +
                "display:inline-block;" +
                "\">Verify Email</a>" +
                "</div>" +
                "<p style=\"font-size:14px;margin-top:20px;color:#666;\">Or copy and paste this link into your browser:</p>" +
                "<p style=\"font-size:12px;color:#0275d8;word-break:break-all;\">" + verificationLink + "</p>" +
                "<p style=\"font-size:14px;margin-top:20px;color:#666;\">This link will expire in 24 hours.</p>" +
                "<p style=\"margin-top:40px;font-size:14px;color:gray;\">Regards,<br>Drac Team</p>" +
                "</div>";
    }

    public static String resetPasswordLinkTemplate(String resetLink) {
        return "<div style=\"max-width:600px;margin:auto;padding:20px;" +
                "font-family:Arial, sans-serif;border-radius:10px;border:1px solid #e0e0e0;\">" +
                "<h2 style=\"color:#d9534f;text-align:center;\">Password Reset Request</h2>" +
                "<p style=\"font-size:16px;\">Hello,</p>" +
                "<p style=\"font-size:16px;\">You requested to reset your password. Click the link below to reset it:</p>" +
                "<div style=\"text-align:center;margin-top:30px;\">" +
                "<a href=\"" + resetLink + "\" style=\"" +
                "padding:12px 25px;" +
                "background:#d9534f;" +
                "color:white;" +
                "text-decoration:none;" +
                "border-radius:5px;" +
                "font-size:16px;" +
                "display:inline-block;" +
                "\">Reset Password</a>" +
                "</div>" +
                "<p style=\"font-size:14px;margin-top:20px;color:#666;\">Or copy and paste this link into your browser:</p>" +
                "<p style=\"font-size:12px;color:#d9534f;word-break:break-all;\">" + resetLink + "</p>" +
                "<p style=\"font-size:14px;margin-top:20px;color:#666;\">This link will expire in 15 minutes.</p>" +
                "<p style=\"font-size:14px;margin-top:20px;color:#d9534f;\">If you did not request this, please ignore this email.</p>" +
                "<p style=\"margin-top:40px;font-size:14px;color:gray;\">Regards,<br>Drac Team</p>" +
                "</div>";
    }
}

