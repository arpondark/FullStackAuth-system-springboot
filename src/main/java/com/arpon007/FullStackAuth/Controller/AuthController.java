package com.arpon007.FullStackAuth.Controller;

import com.arpon007.FullStackAuth.Entity.UserEntity;
import com.arpon007.FullStackAuth.Io.AuthRequest;
import com.arpon007.FullStackAuth.Io.AuthResponse;
import com.arpon007.FullStackAuth.Io.ResetPasswordRequest;
import com.arpon007.FullStackAuth.Service.AppUserDetaisService;
import com.arpon007.FullStackAuth.Service.ProfileService;
import com.arpon007.FullStackAuth.Util.JwtUtil;
import com.arpon007.FullStackAuth.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthController exposes authentication endpoints for the API.
 * <p>
 * All endpoints here are automatically prefixed with /api/v1 (configured in application.properties as server.servlet.context-path).
 * <p>
 * <b>Current Endpoints (Token-Based Verification):</b>
 * <ul>
 *   <li><b>POST /api/v1/auth/login</b> - Authenticates user and issues JWT token</li>
 *   <li><b>GET /api/v1/auth/isAuthenticated</b> - Checks if current user is authenticated</li>
 *   <li><b>GET /api/v1/auth/verify</b> - Verify email using token from verification link</li>
 *   <li><b>POST /api/v1/auth/request-password-reset</b> - Request password reset (sends reset link email)</li>
 *   <li><b>POST /api/v1/auth/reset-password</b> - Reset password using token from reset link</li>
 * </ul>
 * 
 * <p><b>Email Verification Flow (Token-Based):</b></p>
 * <ol>
 *   <li>User signs up → Verification token (UUID) generated automatically</li>
 *   <li>Token logged to console: <code>log.info("Token for {}: {}", email, token)</code></li>
 *   <li>Verification email sent with link: <code>${app.url}/api/auth/verify?token=&lt;token&gt;</code></li>
 *   <li>User clicks link → GET /api/v1/auth/verify → Account activated</li>
 * </ol>
 * 
 * <p><b>Password Reset Flow (Token-Based):</b></p>
 * <ol>
 *   <li>User requests reset → POST /api/v1/auth/request-password-reset</li>
 *   <li>Reset token (UUID) generated and logged</li>
 *   <li>Reset email sent with link: <code>${app.url}/api/auth/reset-password?token=&lt;token&gt;</code></li>
 *   <li>User clicks link and submits new password → POST /api/v1/auth/reset-password</li>
 * </ol>
 * 
 * <p><b>Deprecated Endpoints (Commented Out):</b></p>
 * <ul>
 *   <li><code>POST /api/v1/auth/send-otp</code> - Old OTP-based verification (replaced by automatic token on signup)</li>
 *   <li><code>POST /api/v1/auth/verify-otp</code> - Old OTP verification (replaced by GET /verify with token)</li>
 *   <li><code>POST /api/v1/auth/send-reset-otp</code> - Old OTP-based reset (replaced by POST /request-password-reset)</li>
 * </ul>
 * <p>
 * ═══════════════════════════════════════════════════════════════════════════════════
 * JWT (JSON Web Token) - BEGINNER'S GUIDE
 * ═══════════════════════════════════════════════════════════════════════════════════
 * <p>
 * What is JWT?
 * • A compact string that proves a user is who they claim to be
 * • Contains three parts separated by dots: header.payload.signature
 * • Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature
 * • The token is SIGNED with a secret key, so the server can verify it hasn't been tampered with
 * <p>
 * How JWT Authentication Works:
 * 1. User logs in with email + password → Server validates credentials
 * 2. Server generates a JWT token (valid for 10 hours)
 * 3. Token is returned to client in TWO ways:
 * - In the response body (for non-cookie clients like mobile apps)
 * - As an HTTP-only cookie (for web browsers, more secure against XSS attacks)
 * 4. On every future request, client sends the token:
 * - Option A: In Authorization header as "Bearer <token>"
 * - Option B: In HTTP-only cookie (automatic for web browsers)
 * 5. Server verifies the token signature and checks expiration
 * 6. If valid, server extracts user info (email) and allows the request
 * 7. If invalid or expired, server rejects the request with 401 Unauthorized
 * <p>
 * Why Two Ways (header + cookie)?
 * • Header (Bearer): Works for mobile apps, frontend frameworks, and any client
 * • Cookie: Automatic for browsers, but needs credentials flag in fetch/axios
 * To send cookies in cross-origin requests, use:
 * - fetch: fetch(url, { credentials: 'include' })
 * - axios: axios.create({ withCredentials: true })
 * <p>
 * Security Notes:
 * • httpOnly flag: Prevents JavaScript from reading the cookie (protects against XSS)
 * • SameSite=Lax: Prevents CSRF attacks but still sends cookie in same-site requests
 * • If frontend is on different domain: Change SameSite to None and use credentials: 'include'
 * • Secret key: Kept on server only, never shared. Used to verify token signature.
 * <p>
 * ═══════════════════════════════════════════════════════════════════════════════════
 * <p>
 * Login Flow (Beginner-friendly explanation):
 * 1. Client sends email + password in the request body.
 * 2. We authenticate the credentials using Spring Security's AuthenticationManager.
 * 3. If successful, we load user details and create a signed JWT using JwtUtil.
 * 4. We return the token in two places:
 * - As the response body (AuthResponse) so non-cookie clients can store it
 * - As an HTTP-only cookie named "jwt" to protect against XSS reading the token
 * 5. The client should include the token on future requests (either Authorization header or cookie).
 * <p>
 * Cookie Configuration Explained:
 * • httpOnly(true): JavaScript cannot read the cookie (prevents XSS attacks where malicious JS steals token)
 * • path("/"): Cookie is sent with requests to any path on the server
 * • maxAge(Duration.ofDays(1)): Cookie expires after 1 day
 * • sameSite("Lax"): Cookie is sent with same-site requests and top-level navigation, but not with cross-site requests
 * This prevents CSRF (Cross-Site Request Forgery) attacks while still being usable by the frontend.
 * <p>
 * NOTE: If your frontend is on a different domain (e.g., localhost:3000) and this backend is on localhost:8080,
 * you may need to:
 * - Change sameSite to "None" and set secure(true)
 * - Ensure frontend sends credentials: { credentials: 'include' }
 * - Set CORS allowCredentials to true (already configured in SecurityConfig)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final AppUserDetaisService appUserDetaisService;
    private final JwtUtil jwtUtil;
    private final ProfileService profileService;
    private final UserRepository userRepository;


    /**
     * Authenticates user credentials and returns a JWT token.
     * <p>
     * Request body example:
     * { "email": "user@example.com", "password": "secret" }
     * <p>
     * Success response:
     * - Sets an HTTP-only cookie: jwt=<token>
     * - Body: { "email": "user@example.com", "token": "<jwt>" }
     * <p>
     * Error responses:
     * - 400 Bad Request for invalid credentials
     * - 401 Unauthorized if the account is disabled
     *
     * @param request AuthRequest containing email and password
     * @return ResponseEntity with cookie + AuthResponse or error message
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticate(request.getEmail(), request.getPassword());

            // Check if account is verified before issuing login token
            UserEntity user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (user.getIsAccountVerified() == null || !user.getIsAccountVerified()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", true);
                error.put("message", "Account is not verified. Please verify your email first.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            final UserDetails userDetails = appUserDetaisService.loadUserByUsername(request.getEmail());
            final String jwtToken = jwtUtil.generateToken(userDetails);
            ResponseCookie cookie = ResponseCookie.from("jwt", jwtToken)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofDays(1))
                    .sameSite("Strict")
                    .build();
            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(new AuthResponse(request.getEmail(), jwtToken));

        } catch (BadCredentialsException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "Invalid credentials");
            return ResponseEntity.badRequest().body(error);

        } catch (DisabledException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "Account is disabled");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (UsernameNotFoundException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }


    }

    /**
     * Wraps AuthenticationManager to perform username/password authentication.
     * Throws an exception if credentials are invalid or the account is disabled.
     *
     * @param email    user's email (used as username)
     * @param password raw password
     */
    private void authenticate(String email, String password) {
        authenticationManager.authenticate((new UsernamePasswordAuthenticationToken(email, password)));
    }

    /**
     * User signup - creates account and returns temporary signup JWT cookie.
     *
     * <p><b>Signup + Verification Flow:</b></p>
     * <ol>
     *   <li>User calls POST /api/v1/auth/signup with email, password, name</li>
     *   <li>Server creates user account with isAccountVerified=false</li>
     *   <li>Server generates temporary signup JWT (24-hour expiration)</li>
     *   <li>Server returns signup token in HTTP-only cookie + response body</li>
     *   <li>Server sends verification email with link containing UUID token</li>
     *   <li>User clicks verification link → GET /api/v1/auth/verify?token=&lt;uuid&gt;</li>
     *   <li>User is already authenticated via signup cookie, can access /auth/verify</li>
     *   <li>After verification, isAccountVerified=true, cookie remains valid</li>
     *   <li>User can now login normally or use the signup cookie for immediate access</li>
     * </ol>
     *
     * @param request ProfileRequest containing email, password, name
     * @return ResponseEntity with signup cookie + response body containing token
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody com.arpon007.FullStackAuth.Io.ProfileRequest request) {
        try {
            // Create the user profile (triggers verification email)
            com.arpon007.FullStackAuth.Io.ProfileResponse profileResponse = profileService.createProfile(request);

            // Generate temporary signup JWT token (24-hour expiration)
            String signupToken = profileService.generateSignupToken(request.getEmail());

            // Create HTTP-only cookie with signup token
            ResponseCookie cookie = ResponseCookie.from("jwt", signupToken)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofDays(1))  // 24 hours, matches token expiration
                    .sameSite("Strict")
                    .build();

            // Return response with cookie and token in body
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Signup successful! Check your email to verify your account.");
            response.put("email", request.getEmail());
            response.put("token", signupToken);
            response.put("userId", profileResponse.getUserId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Set-Cookie", cookie.toString())
                    .body(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Checks if the current user is authenticated.
     * <p>
     * How it works:
     * 1. JwtRequestFilter validates the JWT from the Authorization header or cookie
     * 2. If valid, it sets the authentication in SecurityContext with the user email
     * 3. This endpoint checks if SecurityContext has a valid authentication
     * 4. If authentication exists AND is authenticated AND not anonymous, user is logged in
     * <p>
     * Testing:
     * 1. WITHOUT login: GET /api/v1/auth/isAuthenticated → returns false
     * 2. Login: POST /api/v1/auth/login with email + password → get token
     * 3. Copy token and add header: Authorization: Bearer <token>
     * 4. Call endpoint: GET /api/v1/auth/isAuthenticated → returns true
     *
     * @return true if user is authenticated with a valid JWT, false otherwise
     */
    @GetMapping("/isAuthenticated")
    public ResponseEntity<Map<String, Object>> isAuthenticated() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null
                && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"));

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", isAuthenticated);
        if (isAuthenticated) {
            response.put("email", auth.getName());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * DEPRECATED: Old OTP-based password reset request endpoint.
     * This endpoint is kept for backward compatibility but is not recommended.
     * 
     * @deprecated Use {@link #requestPasswordReset(Map)} instead for token-based reset
     */
    /*
    @PostMapping("/send-reset-otp")
    public void sentResetOtp(@RequestParam String email) {
        try {
            profileService.sendResetOpt(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }
    */

    /**
     * Verify email using token from verification link.
     * 
     * <p><b>Verification Flow with Cookie Upgrade:</b></p>
     * <ol>
     *   <li>User receives verification link from email with UUID token</li>
     *   <li>User already has signup cookie from registration</li>
     *   <li>GET /api/v1/auth/verify?token=&lt;uuid&gt; is called</li>
     *   <li>User's email is verified in database (isAccountVerified=true)</li>
     *   <li>Server generates permanent login JWT (10-hour expiration)</li>
     *   <li>Server returns new login token in HTTP-only cookie (replacing signup token)</li>
     *   <li>User can now access all authenticated endpoints with permanent login token</li>
     * </ol>
     *
     * @param token verification token from email link
     * @return success message with upgraded login token
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token) {
        try {
            // Get the user's email from the verification token BEFORE verification clears it
            String email = profileService.getEmailByVerificationToken(token);

            // Verify email using the UUID token from email link
            profileService.verifyEmailByToken(token);


            // Load user details and generate permanent login token
            UserDetails userDetails = appUserDetaisService.loadUserByUsername(email);
            String loginToken = jwtUtil.generateToken(userDetails);

            // Create new HTTP-only cookie with permanent login token (10 hours instead of 24)
            ResponseCookie cookie = ResponseCookie.from("jwt", loginToken)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(Duration.ofHours(10))  // Permanent login token duration
                    .sameSite("Strict")
                    .build();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email verified successfully! You are now logged in.");
            response.put("email", email);
            response.put("token", loginToken);

            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Request password reset - generates token and sends reset link via email.
     * 
     * @param request body containing email
     * @return success message
     */
    @PostMapping("/request-password-reset")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", true);
                error.put("message", "Email is required");
                return ResponseEntity.badRequest().body(error);
            }
            profileService.sendResetOpt(email);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset link has been sent to your email");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Reset password using token from reset link.
     * Accepts token either as query parameter or in request body.
     * 
     * @param token token from reset link (query parameter, optional if in body)
     * @param request body containing newPassword (required) and optionally token
     * @return success message
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestParam(required = false) String token,
            @RequestBody Map<String, String> request) {
        try {
            String resetToken = token;
            String newPassword = request.get("newPassword");
            
            // If token not in query param, get it from request body
            if (resetToken == null || resetToken.isEmpty()) {
                resetToken = request.get("token");
            }
            
            if (resetToken == null || resetToken.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", true);
                error.put("message", "Token is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (newPassword == null || newPassword.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", true);
                error.put("message", "New password is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            profileService.resetPasswordByToken(resetToken, newPassword);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * DEPRECATED: Old OTP-based email verification endpoint.
     * Email verification now happens automatically on signup with token-based links.
     * 
     * @deprecated Email verification is automatic on signup. Use GET /verify with token instead.
     * @see #verifyEmail(String) - Token-based verification endpoint
     */
    /*
    @PostMapping("/send-otp")
    public void sendverifyOtp(@CurrentSecurityContext(expression = "authentication?.name") String email) {
        try {
            profileService.sentOtp(email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    */

    /**
     * DEPRECATED: Old OTP-based email verification endpoint.
     * This endpoint is no longer used. Email verification now uses token-based links.
     * 
     * @deprecated Use {@link #verifyEmail(String)} instead for token-based verification
     */
    /*
    @PostMapping("/verify-otp")
    public void verifyEmail(@RequestBody Map<String, Object> request, @CurrentSecurityContext(expression = "authentication?.name") String email) {
        if(request.get("otp") == null){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is required/Invalid");
        }
        try {
            profileService.verifyOtp(email, request.get("otp").toString());
            log.info("OTP verified for email: {}", email);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
    */
}
