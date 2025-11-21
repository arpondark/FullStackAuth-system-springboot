package com.arpon007.FullStackAuth.Service.Profile;

import com.arpon007.FullStackAuth.Entity.Role.RoleEntity;
import com.arpon007.FullStackAuth.Entity.UserEntity;
import com.arpon007.FullStackAuth.Io.Profile.ProfileUpdateRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileResponse;
import com.arpon007.FullStackAuth.Io.Profile.ChangePasswordRequest;
import com.arpon007.FullStackAuth.Io.Profile.VerifyChangePasswordRequest;
import com.arpon007.FullStackAuth.Io.Profile.ChangeEmailRequest;
import com.arpon007.FullStackAuth.Io.Profile.VerifyChangeEmailRequest;
import java.util.concurrent.ThreadLocalRandom;
import com.arpon007.FullStackAuth.Service.Email.EmailService;
import com.arpon007.FullStackAuth.Util.JwtUtil;
import com.arpon007.FullStackAuth.repository.UserRepository;
import com.arpon007.FullStackAuth.repository.RoleRepository;
import com.arpon007.FullStackAuth.Entity.Role.RoleName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * ProfileServiceImpl handles user profile operations including registration,
 * email verification, and password reset.
 * 
 * <p><b>Current Implementation (Token-Based):</b></p>
 * <ul>
 *   <li><b>Email Verification:</b> Uses UUID tokens sent via email links</li>
 *   <li><b>Password Reset:</b> Uses UUID tokens sent via email links</li>
 *   <li><b>Token Logging:</b> All generated tokens are logged to console</li>
 * </ul>
 * 
 * <p><b>Email Verification Flow:</b></p>
 * <ol>
 *   <li>User signs up → Verification token (UUID) generated automatically</li>
 *   <li>Token logged: <code>log.info("Token for {}: {}", email, token)</code></li>
 *   <li>Verification link sent: <code>${app.url}/api/auth/verify?token=&lt;token&gt;</code></li>
 *   <li>User clicks link → Account activated</li>
 * </ol>
 * 
 * <p><b>Password Reset Flow:</b></p>
 * <ol>
 *   <li>User requests reset → Reset token (UUID) generated</li>
 *   <li>Token logged: <code>log.info("Token for {}: {}", email, token)</code></li>
 *   <li>Reset link sent: <code>${app.url}/api/auth/reset-password?token=&lt;token&gt;</code></li>
 *   <li>User clicks link and sets new password</li>
 * </ol>
 * 
 * <p><b>Deprecated Methods (Commented Out):</b></p>
 * <ul>
 *   <li><code>sentOtp()</code> - Old OTP-based verification (replaced by token-based)</li>
 *   <li><code>verifyOtp()</code> - Old OTP verification (replaced by token-based)</li>
 *   <li><code>resetPassword(String email, String otp, String newPassword)</code> - Old OTP-based reset (kept for backward compatibility)</li>
 * </ul>
 * 
 * @author Arpon007
 * @version 2.0 - Token-based verification and reset
 */
@Service
@Data
@AllArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {


    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RoleRepository roleRepository;

    @Override
    public ProfileResponse createProfile(ProfileRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            UserEntity existingUser = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // If user exists but not verified, provide helpful message
            if (existingUser.getIsAccountVerified() == null || !existingUser.getIsAccountVerified()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already registered but not verified. Please check your email or use the resend verification endpoint.");
            }

            // If user exists and is verified
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists and is verified. Please login instead.");
        }

        // Create new user
        UserEntity newProfile = convertToUserEntity(request);

        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours

        newProfile.setVerifyOtp(verificationToken);
        newProfile.setVerifyOtpExpireAt(expiryTime);

        newProfile = userRepository.save(newProfile);

        // Log the token
        log.info("Token for {}: {}", request.getEmail(), verificationToken);

        // Send verification link email
        emailService.sendVerificationLinkEmail(request.getEmail(), verificationToken);

        return convertToUserResponse(newProfile);
    }

    @Override
    public ProfileResponse getProfile(String email) {
        UserEntity existUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return convertToUserResponse(existUser);
    }

    @Override
    public ProfileResponse updateProfile(String email, ProfileUpdateRequest request) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        if (request.getName() != null && !request.getName().isBlank()) {
            existingUser.setName(request.getName());
        }
        existingUser = userRepository.save(existingUser);
        return convertToUserResponse(existingUser);
    }

    @Override
    public void sendResetOpt(String email) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        
        // Expire time - 15 minutes
        long expireTime = System.currentTimeMillis() + (15 * 60 * 1000);
        existingUser.setResetOtpExpireAt(expireTime);
        existingUser.setResetOtp(resetToken);
        userRepository.save(existingUser);
        
        // Log the token
        log.info("Token for {}: {}", email, resetToken);
        
        // Send reset link email
        emailService.sendPasswordResetLinkEmail(email, resetToken);
    }

    /**
     * DEPRECATED: Old OTP-based password reset method.
     * Kept for backward compatibility but not recommended for new implementations.
     * 
     * @deprecated Use {@link #resetPasswordByToken(String, String)} instead
     */
    @Override
    @Deprecated
    public void resetPassword(String email, String otp, String newPassword) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        if (existingUser.getResetOtp() == null || !existingUser.getResetOtp().equals(otp)) {
            throw new RuntimeException("Invalid token");
        }
        if (existingUser.getResetOtpExpireAt() < System.currentTimeMillis()) {
            throw new RuntimeException("Token has expired");
        }
        existingUser.setPassword(passwordEncoder.encode(newPassword));
        existingUser.setResetOtp(null);
        existingUser.setResetOtpExpireAt(0L);
        userRepository.save(existingUser);
    }
    
    public void resetPasswordByToken(String token, String newPassword) {
        UserEntity existingUser = userRepository.findByResetOtp(token)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid or expired token"));
        
        if (existingUser.getResetOtpExpireAt() < System.currentTimeMillis()) {
            throw new RuntimeException("Token has expired");
        }
        
        existingUser.setPassword(passwordEncoder.encode(newPassword));
        existingUser.setResetOtp(null);
        existingUser.setResetOtpExpireAt(0L);
        userRepository.save(existingUser);
    }
    
    public void verifyEmailByToken(String token) {
        UserEntity existingUser = userRepository.findByVerifyOtp(token)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid or expired token"));
        
        if (existingUser.getVerifyOtpExpireAt() < System.currentTimeMillis()) {
            throw new RuntimeException("Token has expired");
        }
        
        existingUser.setIsAccountVerified(true);
        existingUser.setVerifyOtp(null);
        existingUser.setVerifyOtpExpireAt(0L);
        userRepository.save(existingUser);
    }

    /**
     * DEPRECATED: Old OTP-based email verification method.
     * This method is no longer used. Email verification now happens automatically
     * during signup with token-based links.
     * 
     * @deprecated Email verification is now automatic on signup. Use token-based verification instead.
     * @see #createProfile(ProfileRequest) - Automatically generates verification token
     * @see #verifyEmailByToken(String) - Token-based verification
     */
    /*
    @Override
    public void sentOtp(String email) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        if (existingUser.getIsAccountVerified() != null && existingUser.getIsAccountVerified()) {
            return;
            //throw new RuntimeException("Account already verified");
        }
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); //expire time set here

        existingUser.setVerifyOtp(otp);
        existingUser.setVerifyOtpExpireAt(expiryTime);
        userRepository.save(existingUser);
        try {
            emailService.sendVerificationOtpEmail(existingUser.getEmail(), otp);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    */

    /**
     * DEPRECATED: Old OTP-based email verification method.
     * This method is no longer used. Email verification now uses token-based links.
     * 
     * @deprecated Use {@link #verifyEmailByToken(String)} instead for token-based verification
     */
    /*
    @Override
    public void verifyOtp(String email, String otp) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        if (existingUser.getVerifyOtp() == null || !existingUser.getVerifyOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        if (existingUser.getVerifyOtpExpireAt() < System.currentTimeMillis()) {
            throw new RuntimeException("OTP has expired");
        }
        existingUser.setIsAccountVerified(true);
        existingUser.setVerifyOtp(null);
        existingUser.setVerifyOtpExpireAt(0L);

        userRepository.save(existingUser);

    }
    */


    @Override
    public String getLoggedinUserId(String email) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return existingUser.getUserId();
    }

    private ProfileResponse convertToUserResponse(UserEntity newProfile) {
        return ProfileResponse.builder()
                .userId(newProfile.getUserId())
                .name(newProfile.getName())
                .email(newProfile.getEmail())
                .isAccountVerified(newProfile.getIsAccountVerified())
                .build();

    }

    private UserEntity convertToUserEntity(ProfileRequest request) {
        var userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name(RoleName.USER).build()));

        return UserEntity.builder()
                .email(request.getEmail())
                .userId(UUID.randomUUID().toString())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .isAccountVerified(false)
                .resetOtpExpireAt(0L)
                .verifyOtp(null)
                .verifyOtpExpireAt(0L)
                .resetOtp(null)
                .roles(new java.util.HashSet<>(java.util.List.of(userRole)))
                .build();

    }

    @Override
    public String generateSignupToken(String email) {
        return jwtUtil.generateSignupToken(email);
    }

    @Override
    public String getEmailByVerificationToken(String verificationToken) {
        UserEntity existingUser = userRepository.findByVerifyOtp(verificationToken)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid or expired token"));
        return existingUser.getEmail();
    }

    @Override
    public String getEmailByResetToken(String resetToken) {
        UserEntity existingUser = userRepository.findByResetOtp(resetToken)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid or expired token"));
        return existingUser.getEmail();
    }

    @Override
    public void resendVerificationEmail(String email) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Check if account is already verified
        if (existingUser.getIsAccountVerified() != null && existingUser.getIsAccountVerified()) {
            throw new IllegalStateException("Account is already verified. Please login instead.");
        }

        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours

        existingUser.setVerifyOtp(verificationToken);
        existingUser.setVerifyOtpExpireAt(expiryTime);
        userRepository.save(existingUser);

        // Log the token
        log.info("Resend verification token for {}: {}", email, verificationToken);

        // Send verification link email
        emailService.sendVerificationLinkEmail(email, verificationToken);
    }

    @Override
    public void initiateChangePassword(String email, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid old password");
        }

        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000); // 15 minutes

        user.setResetOtp(otp);
        user.setResetOtpExpireAt(expiryTime);
        userRepository.save(user);

        log.info("Change Password OTP for {}: {}", email, otp);
        emailService.sendResetOtpEmail(email, otp);
    }

    @Override
    public void completeChangePassword(String email, VerifyChangePasswordRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(request.getOtp())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        if (user.getResetOtpExpireAt() < System.currentTimeMillis()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpireAt(0L);
        userRepository.save(user);
    }

    @Override
    public void initiateChangeEmail(String email, ChangeEmailRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password");
        }

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        long expiryTime = System.currentTimeMillis() + (15 * 60 * 1000); // 15 minutes

        user.setPendingEmail(request.getNewEmail());
        user.setVerifyOtp(otp);
        user.setVerifyOtpExpireAt(expiryTime);
        userRepository.save(user);

        log.info("Change Email OTP for {} (sent to {}): {}", email, request.getNewEmail(), otp);
        emailService.sendVerificationOtpEmail(request.getNewEmail(), otp);
    }

    @Override
    public void completeChangeEmail(String email, VerifyChangeEmailRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getVerifyOtp() == null || !user.getVerifyOtp().equals(request.getOtp())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
        }

        if (user.getVerifyOtpExpireAt() < System.currentTimeMillis()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired");
        }

        if (user.getPendingEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending email change found");
        }

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setVerifyOtp(null);
        user.setVerifyOtpExpireAt(0L);
        userRepository.save(user);
    }
}
