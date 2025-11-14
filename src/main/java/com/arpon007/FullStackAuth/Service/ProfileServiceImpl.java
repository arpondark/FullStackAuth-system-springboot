package com.arpon007.FullStackAuth.Service;

import com.arpon007.FullStackAuth.Entity.UserEntity;
import com.arpon007.FullStackAuth.Io.ProfileRequest;
import com.arpon007.FullStackAuth.Io.ProfileResponse;
import com.arpon007.FullStackAuth.Util.JwtUtil;
import com.arpon007.FullStackAuth.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    @Override
    public ProfileResponse createProfile(ProfileRequest request) {
        UserEntity newProfile = convertToUserEntity(request);
        if (!userRepository.existsByEmail(request.getEmail())) {
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
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    @Override
    public ProfileResponse getProfile(String email) {
        UserEntity existUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return convertToUserResponse(existUser);
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
}
