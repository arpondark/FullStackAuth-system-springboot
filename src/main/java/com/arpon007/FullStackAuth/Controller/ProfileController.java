package com.arpon007.FullStackAuth.Controller;


import com.arpon007.FullStackAuth.Io.Profile.ProfileUpdateRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileUpdateRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileResponse;
import com.arpon007.FullStackAuth.Io.Profile.ChangePasswordRequest;
import com.arpon007.FullStackAuth.Io.Profile.VerifyChangePasswordRequest;
import com.arpon007.FullStackAuth.Io.Profile.ChangeEmailRequest;
import com.arpon007.FullStackAuth.Io.Profile.VerifyChangeEmailRequest;
import com.arpon007.FullStackAuth.Service.Email.EmailService;
import com.arpon007.FullStackAuth.Service.Profile.ProfileService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

/**
 * ProfileController manages user registration and profile operations.
 * Note: Registration is public (/api/v1.1/auth/register).
 * Other profile endpoints are protected (require JWT).
 *
 * IMPORTANT - JWT Authentication Flow:
 * 1. Login at POST /api/v1.1/auth/login with email + password
 * 2. You will receive:
 *    - Response body with token
 *    - HTTP-only cookie "jwt" (automatically set)
 * 3. For future requests:
 *    - Option A: Include "Authorization: Bearer <token>" header
 *    - Option B: Cookie is automatically sent by browser
 * 4. Test if authenticated: GET /api/v1.1/auth/isAuthenticated
 * 5. Test protected endpoint: GET /api/v1.1/test
 *
 * Common Issues:
 * - Cookie not being sent? Make sure frontend uses { credentials: 'include' } in fetch/axios
 * - Getting 401? Token might be expired (valid for 10 hours) or invalid
 * - Getting 403? You're authenticated but not authorized for this endpoint
 */
@RestController
@Data
@AllArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final EmailService emailService;


    @GetMapping("/test")
    public String test() {

        return "ok Running";
    }

    /**
     * Register a new user account (PUBLIC - no JWT required).
     */
    @PostMapping("/auth/register")
    @ResponseStatus(code = CREATED)
    public ProfileResponse createProfile(@Valid @RequestBody ProfileRequest request) {
        // Verification email is automatically sent in ProfileServiceImpl.createProfile()
        return profileService.createProfile(request);
    }


//    @GetMapping("/api/v1/profile/me")
//    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("email", userDetails.getUsername());
//        response.put("authorities", userDetails.getAuthorities());
//        response.put("message", "✅ JWT Cookie is working! You are authenticated.");
//        return ResponseEntity.ok(response);
//    }
    @GetMapping("/profile/me")
    public ProfileResponse getCurrentUser(@CurrentSecurityContext(expression = "authentication?.name") String email) {
        return profileService.getProfile(email);
    }

    @PutMapping("/profile/me")
    public ProfileResponse updateCurrentUser(
            @CurrentSecurityContext(expression = "authentication?.name") String email,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(email, request);
    }

    @PostMapping("/profile/change-password/init")
    public void initiateChangePassword(
            @CurrentSecurityContext(expression = "authentication?.name") String email,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.initiateChangePassword(email, request);
    }

    @PostMapping("/profile/change-password/verify")
    public void completeChangePassword(
            @CurrentSecurityContext(expression = "authentication?.name") String email,
            @Valid @RequestBody VerifyChangePasswordRequest request) {
        profileService.completeChangePassword(email, request);
    }

    @PostMapping("/profile/change-email/init")
    public void initiateChangeEmail(
            @CurrentSecurityContext(expression = "authentication?.name") String email,
            @Valid @RequestBody ChangeEmailRequest request) {
        profileService.initiateChangeEmail(email, request);
    }

    @PostMapping("/profile/change-email/verify")
    public void completeChangeEmail(
            @CurrentSecurityContext(expression = "authentication?.name") String email,
            @Valid @RequestBody VerifyChangeEmailRequest request) {
        profileService.completeChangeEmail(email, request);
    }

}
