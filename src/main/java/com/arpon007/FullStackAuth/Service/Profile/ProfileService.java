package com.arpon007.FullStackAuth.Service.Profile;

import com.arpon007.FullStackAuth.Io.Profile.ProfileUpdateRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileResponse;
import com.arpon007.FullStackAuth.Io.Profile.ChangePasswordRequest;
import com.arpon007.FullStackAuth.Io.Profile.VerifyChangePasswordRequest;
import com.arpon007.FullStackAuth.Io.Profile.ChangeEmailRequest;
import com.arpon007.FullStackAuth.Io.Profile.VerifyChangeEmailRequest;

public interface ProfileService {
    ProfileResponse createProfile(ProfileRequest request);

    ProfileResponse getProfile(String email);

    void sendResetOpt(String email);
    void resetPassword(String email, String otp, String newPassword);
    void resetPasswordByToken(String token, String newPassword);
    void verifyEmailByToken(String token);

    void initiateChangePassword(String email, ChangePasswordRequest request);
    void completeChangePassword(String email, VerifyChangePasswordRequest request);
    void initiateChangeEmail(String email, ChangeEmailRequest request);
    void completeChangeEmail(String email, VerifyChangeEmailRequest request);

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

    /**
     * Resends the verification email to an unverified user.
     * Generates a new verification token and sends a new email.
     *
     * @param email the user's email address
     * @throws UsernameNotFoundException if user not found
     * @throws IllegalStateException if account is already verified
     */
    void resendVerificationEmail(String email);

    ProfileResponse updateProfile(String email, ProfileUpdateRequest request);
}
