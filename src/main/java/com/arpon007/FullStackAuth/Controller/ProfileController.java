package com.arpon007.FullStackAuth.Controller;


import com.arpon007.FullStackAuth.Io.ProfileRequest;
import com.arpon007.FullStackAuth.Io.ProfileResponse;
import com.arpon007.FullStackAuth.Service.EmailService;
import com.arpon007.FullStackAuth.Service.ProfileService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Test endpoint - requires authentication.
     * Use this to verify JWT is working correctly.
     *
     * How to test:
     * 1. Login: POST /api/v1.1/auth/login with credentials
     * 2. Copy the token from response
     * 3. Call this endpoint with: Authorization: Bearer <token>
     * 4. Should return: { "message": "Hello, authenticated user!" }
     *
     * @param email from SecurityContext (set by JwtRequestFilter)
     * @return success message
     */
    @GetMapping("/test")
    public ResponseEntity<?> test(@CurrentSecurityContext(expression = "authentication?.name") String email) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, authenticated user!");
        response.put("email", email);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Register a new user account (PUBLIC - no JWT required).
     */
    @PostMapping("/auth/register")
    @ResponseStatus(code = CREATED)
    public ProfileResponse createProfile(@Valid @RequestBody ProfileRequest request) {
        ProfileResponse response= profileService.createProfile(request);
        emailService.sendWelcomeEmail(response.getEmail(),response.getName());
        return response;
    }


//    @GetMapping("/api/v1/profile/me")
//    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("email", userDetails.getUsername());
//        response.put("authorities", userDetails.getAuthorities());
//        response.put("message", "✅ JWT Cookie is working! You are authenticated.");
//        return ResponseEntity.ok(response);
//    }
    @GetMapping("/api/v1/profile/me")
    public ProfileResponse getCurrentUser(@CurrentSecurityContext(expression = "authentication?.name") String email) {
        return profileService.getProfile(email);
    }

}
