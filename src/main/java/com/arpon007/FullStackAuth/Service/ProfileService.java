package com.arpon007.FullStackAuth.Service;

import com.arpon007.FullStackAuth.Io.ProfileRequest;
import com.arpon007.FullStackAuth.Io.ProfileResponse;

public interface ProfileService {
    ProfileResponse createProfile(ProfileRequest request);

    ProfileResponse getProfile(String email);

    void sendResetOpt(String email);
    void resetPassword(String email, String otp, String newPassword);
    void resetPasswordByToken(String token, String newPassword);
    void verifyEmailByToken(String token);

    String getLoggedinUserId(String email);

    /**
     * Generates a temporary JWT token for unverified users during signup.
     * This token is used for email verification and has 24-hour expiration.
     *
     * @param email the user's email address
     * @return JWT token string
     */
    String generateSignupToken(String email);

    /**
     * Extracts the email address from a verification token.
     * Used to get the user's email after they click the verification link.
     *
     * @param verificationToken the UUID verification token from email link
     * @return the user's email address
     */
    String getEmailByVerificationToken(String verificationToken);

    /**
     * Extracts the email address from a password reset token.
     * Used to get the user's email from the reset link.
     *
     * @param resetToken the UUID reset token from reset email link
     * @return the user's email address
     */
    String getEmailByResetToken(String resetToken);
}
