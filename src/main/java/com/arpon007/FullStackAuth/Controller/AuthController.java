package com.arpon007.FullStackAuth.Controller;

import com.arpon007.FullStackAuth.Io.AuthRequest;
import com.arpon007.FullStackAuth.Io.AuthResponse;
import com.arpon007.FullStackAuth.Service.AppUserDetaisService;
import com.arpon007.FullStackAuth.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * AuthController exposes authentication endpoints for the API.
 *
 * All endpoints here are automatically prefixed with /api/v1.1 (configured in application.properties as server.servlet.context-path).
 *
 * Endpoints covered here:
 * - POST /api/v1.1/auth/login : Authenticates the user and issues a JWT token.
 * - GET /api/v1.1/auth/isAuthenticated : Checks if current user is authenticated.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * JWT (JSON Web Token) - BEGINNER'S GUIDE
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * What is JWT?
 * • A compact string that proves a user is who they claim to be
 * • Contains three parts separated by dots: header.payload.signature
 * • Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature
 * • The token is SIGNED with a secret key, so the server can verify it hasn't been tampered with
 *
 * How JWT Authentication Works:
 * 1. User logs in with email + password → Server validates credentials
 * 2. Server generates a JWT token (valid for 10 hours)
 * 3. Token is returned to client in TWO ways:
 *    - In the response body (for non-cookie clients like mobile apps)
 *    - As an HTTP-only cookie (for web browsers, more secure against XSS attacks)
 * 4. On every future request, client sends the token:
 *    - Option A: In Authorization header as "Bearer <token>"
 *    - Option B: In HTTP-only cookie (automatic for web browsers)
 * 5. Server verifies the token signature and checks expiration
 * 6. If valid, server extracts user info (email) and allows the request
 * 7. If invalid or expired, server rejects the request with 401 Unauthorized
 *
 * Why Two Ways (header + cookie)?
 * • Header (Bearer): Works for mobile apps, frontend frameworks, and any client
 * • Cookie: Automatic for browsers, but needs credentials flag in fetch/axios
 *   To send cookies in cross-origin requests, use:
 *   - fetch: fetch(url, { credentials: 'include' })
 *   - axios: axios.create({ withCredentials: true })
 *
 * Security Notes:
 * • httpOnly flag: Prevents JavaScript from reading the cookie (protects against XSS)
 * • SameSite=Lax: Prevents CSRF attacks but still sends cookie in same-site requests
 * • If frontend is on different domain: Change SameSite to None and use credentials: 'include'
 * • Secret key: Kept on server only, never shared. Used to verify token signature.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Login Flow (Beginner-friendly explanation):
 * 1. Client sends email + password in the request body.
 * 2. We authenticate the credentials using Spring Security's AuthenticationManager.
 * 3. If successful, we load user details and create a signed JWT using JwtUtil.
 * 4. We return the token in two places:
 *    - As the response body (AuthResponse) so non-cookie clients can store it
 *    - As an HTTP-only cookie named "jwt" to protect against XSS reading the token
 * 5. The client should include the token on future requests (either Authorization header or cookie).
 *
 * Cookie Configuration Explained:
 * • httpOnly(true): JavaScript cannot read the cookie (prevents XSS attacks where malicious JS steals token)
 * • path("/"): Cookie is sent with requests to any path on the server
 * • maxAge(Duration.ofDays(1)): Cookie expires after 1 day
 * • sameSite("Lax"): Cookie is sent with same-site requests and top-level navigation, but not with cross-site requests
 *   This prevents CSRF (Cross-Site Request Forgery) attacks while still being usable by the frontend.
 *
 *   NOTE: If your frontend is on a different domain (e.g., localhost:3000) and this backend is on localhost:8080,
 *   you may need to:
 *   - Change sameSite to "None" and set secure(true)
 *   - Ensure frontend sends credentials: { credentials: 'include' }
 *   - Set CORS allowCredentials to true (already configured in SecurityConfig)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final AppUserDetaisService appUserDetaisService;
    private final JwtUtil jwtUtil;

    /**
     * Authenticates user credentials and returns a JWT token.
     *
     * Request body example:
     * { "email": "user@example.com", "password": "secret" }
     *
     * Success response:
     * - Sets an HTTP-only cookie: jwt=<token>
     * - Body: { "email": "user@example.com", "token": "<jwt>" }
     *
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
        }


    }

    /**
     * Wraps AuthenticationManager to perform username/password authentication.
     * Throws an exception if credentials are invalid or the account is disabled.
     *
     * @param email user's email (used as username)
     * @param password raw password
     */
    private void authenticate(String email, String password) {
        authenticationManager.authenticate((new UsernamePasswordAuthenticationToken(email, password)));
    }

    /**
     * Checks if the current user is authenticated.
     *
     * How it works:
     * 1. JwtRequestFilter validates the JWT from the Authorization header or cookie
     * 2. If valid, it sets the authentication in SecurityContext with the user email
     * 3. This endpoint checks if SecurityContext has a valid authentication
     * 4. If authentication exists AND is authenticated, user is logged in
     *
     * Testing:
     * 1. WITHOUT login: GET /api/v1.1/auth/isAuthenticated → returns false
     * 2. Login: POST /api/v1.1/auth/login with email + password → get token
     * 3. Copy token and add header: Authorization: Bearer <token>
     * 4. Call endpoint: GET /api/v1.1/auth/isAuthenticated → returns true
     *
     * @return true if user is authenticated with a valid JWT, false otherwise
     */
    @GetMapping("/isAuthenticated")
    public ResponseEntity<Boolean> isAuthenticated(){
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated();
        return ResponseEntity.ok(isAuthenticated);
    }
}
