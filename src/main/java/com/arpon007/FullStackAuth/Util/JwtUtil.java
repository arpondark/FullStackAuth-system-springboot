package com.arpon007.FullStackAuth.Util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtUtil is a small helper class for creating and validating JSON Web Tokens (JWTs).
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * WHAT IS A JWT? (Beginner's Explanation)
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * JWT = JSON Web Token. It's a digital ID card that proves a user is who they claim to be.
 *
 * Structure: A JWT has THREE parts separated by dots (.)
 * 1. Header: Says this is a JWT and how it's signed (algorithm)
 * 2. Payload: Contains user info (email, roles, etc.) as JSON
 * 3. Signature: A special code that proves the token hasn't been tampered with
 *
 * Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature
 *          [          Header           ].[          Payload          ].[  Signature  ]
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * HOW DOES JWT WORK? (The Flow)
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * 1. User logs in: Client sends email + password to POST /api/v1.1/auth/login
 * 2. Server validates: If correct, server generates a JWT token
 * 3. Server returns: Token sent to client (in response body AND as HTTP-only cookie)
 * 4. Client stores: Automatically in cookie OR manually in localStorage/sessionStorage
 * 5. Future requests: Client sends token on every request (in header OR cookie)
 * 6. Server validates:
 *    - Checks signature (ensures nobody tampered with the token)
 *    - Checks expiration (is the token still valid?)
 *    - Checks claims (does the email match the user making the request?)
 * 7. If valid: Request is allowed. If invalid: Request is rejected with 401
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * KEY METHODS IN THIS CLASS (What Each Function Does)
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * generateToken(UserDetails userDetails)
 *   → Creates a new JWT token for a logged-in user
 *   → Called in AuthController.login() after successful authentication
 *   → Returns a signed token string
 *
 * validateToken(String token, UserDetails userDetails)
 *   → Checks if a token is legitimate and hasn't expired
 *   → Called by JwtRequestFilter before allowing access to protected endpoints
 *   → Returns true if token is valid, false otherwise
 *
 * extractEmail(String token)
 *   → Pulls out the email from a token (without needing a password!)
 *   → This is how we know WHO is making the request
 *
 * isTokenExpired(String token)
 *   → Checks if a token's time has expired
 *   → Tokens are valid for 10 hours from creation
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * SECURITY ALGORITHM: HS256 (HMAC-SHA256)
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * This app uses HS256 algorithm to sign tokens:
 * • Secret key (kept on server only): Used to create and verify the signature
 * • Hash algorithm (SHA256): Mathematical function that creates the signature
 * • Nobody can fake a token without knowing the secret key
 * • If someone modifies even 1 character, signature becomes invalid
 *
 * Think of it like a tamper-proof seal on an envelope - if someone opens and
 * reseal it, the seal will look different because they don't have the same glue!
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * CONFIGURATION
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Secret key: Defined in application.properties as "jwt.secret.key"
 *   - Must be at least 32 bytes (256 bits) long for HS256
 *   - NEVER commit to git or expose publicly
 *   - In production: Load from environment variables or secret manager
 *
 * Expiration: Set to 10 hours (adjust in createToken() method if needed)
 *   - After 10 hours, token expires and user must log in again
 *   - This is a security measure: old tokens can't be reused indefinitely
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * TROUBLESHOOTING
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * "401 Unauthorized" error?
 *   → Token missing, expired, or invalid signature
 *   → Check if token is being sent in Authorization header or cookie
 *   → Check if browser is sending cookies with { credentials: 'include' }
 *
 * "Token expired" after 10 hours?
 *   → This is NORMAL. User must log in again to get a fresh token.
 *   → Optional: Implement a refresh token mechanism
 *
 * "Signature verification failed"?
 *   → Token was tampered with or created with different secret key
 *   → Check jwt.secret.key in application.properties
 */
@Component
@Data
@RequiredArgsConstructor
public class JwtUtil {
    @Value("${jwt.secret.key}")
    private String secretKey;

    /**
     * Creates a signed JWT for the given user.
     *
     * Claims included:
     * - subject: the user email/username (from UserDetails.getUsername())
     * - iat (issued at): current time
     * - exp (expiration): 10 hours from now
     *
     * @param userDetails the authenticated user's details
     * @return a compact JWT string (e.g., eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...)
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Internal helper to build and sign a JWT with the provided claims and subject (email).
     * Uses HS256 algorithm with the configured secret key.
     *
     * @param claims additional custom claims to embed in the token
     * @param email  subject for the token (usually the user's email/username)
     * @return signed JWT string
     */
    private String createToken(Map<String, Object> claims, String email) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parses the token and returns all claims if signature is valid. Throws if invalid.
     *
     * @param token the JWT to parse
     * @return Claims contained in the token (subject, expiration, etc.)
     */
    private Claims extractAllclaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts a specific claim from the token using a resolver function.
     *
     * @param token          the JWT string
     * @param claimsResolver function that maps Claims -> desired type
     * @param <T>            result type
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllclaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Convenience method to get the subject (email/username) from the token.
     *
     * @param token the JWT string
     * @return subject (the user email/username) stored in the token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Returns the token's expiration time.
     *
     * @param token the JWT string
     * @return expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Checks if the token is expired based on its exp claim.
     *
     * @param token the JWT string
     * @return true if expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates the token for the given user.
     *
     * Validation steps:
     * - Subject in token matches the user's username/email
     * - Token is not expired
     * - Signature is valid (checked when parsing claims)
     *
     * @param token       the JWT string to validate
     * @param userDetails the user to compare against
     * @return true if the token is valid for this user, false otherwise
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
