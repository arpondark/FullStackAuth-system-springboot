package com.arpon007.FullStackAuth.Filter;

import com.arpon007.FullStackAuth.Service.AppUserDetaisService;
import com.arpon007.FullStackAuth.Util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtRequestFilter runs once per HTTP request (extends OncePerRequestFilter) and is responsible for:
 * <ul>
 *   <li>Skipping authentication for public (non-protected) URLs</li>
 *   <li>Extracting the JWT token from the Authorization header (Bearer scheme) or an HTTP-only cookie named "jwt"</li>
 *   <li>Validating the token (signature, expiration, subject match)</li>
 *   <li>Loading the user details and placing an Authentication object into the SecurityContext if valid</li>
 * </ul>
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * WHY IS THIS FILTER NEEDED? (Beginner's Explanation)
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Without this filter:
 *   • Spring Security wouldn't know who the user is on protected endpoints
 *   • Controllers would receive requests but have no user context
 *   • Every endpoint would need to manually extract and validate the JWT
 *
 * With this filter:
 *   • Every request is automatically validated before reaching the controller
 *   • If JWT is valid, user info is automatically available to controllers
 *   • Controllers can use @CurrentSecurityContext to get the logged-in user's email
 *   • This is called "stateless authentication" - no server sessions needed!
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * HOW THIS FILTER WORKS (Step by Step)
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * Step 1: Check if request is PUBLIC
 *   → If path is in PUBLIC_URLS list (/auth/login, /auth/register, /test, etc.)
 *   → Let it through without checking JWT
 *   → This allows users to login and register without a token
 *
 * Step 2: Try to extract JWT from Authorization header
 *   → Look for header: "Authorization: Bearer <token>"
 *   → Extract the token (remove "Bearer " prefix)
 *
 * Step 3: If no header token, try to get JWT from cookie
 *   → Look for HTTP-only cookie named "jwt"
 *   → This is the cookie set by login endpoint
 *   → Browsers automatically send cookies, so this works seamlessly
 *
 * Step 4: Validate the token
 *   → Extract email from token using JwtUtil.extractEmail()
 *   → Load user from database using AppUserDetailsService
 *   → Call JwtUtil.validateToken() to verify:
 *      - Signature is valid (token hasn't been tampered with)
 *      - Email in token matches the database user
 *      - Token hasn't expired
 *
 * Step 5: Set authentication in SecurityContext
 *   → Create a UsernamePasswordAuthenticationToken
 *   → Put it in SecurityContextHolder (like "this is the current user")
 *   → Now controllers can access user info via @CurrentSecurityContext
 *
 * Step 6: Continue to next filter
 *   → Either authenticated (if token was valid) or anonymous
 *   → If anonymous and endpoint is protected, it will be rejected later
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * WHAT IS SECURITYCONTEXT? (Important Concept)
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * SecurityContextHolder = A container that holds the current user's information
 *
 * Think of it like a "current user" variable that's available everywhere in the app:
 *   • Set by JwtRequestFilter when JWT is valid
 *   • Used by @CurrentSecurityContext annotation to access user email
 *   • Used by Spring Security to check if user is authorized for endpoint
 *   • Each request gets its own SecurityContext (thread-safe)
 *
 * Example: When a controller has
 *   @GetMapping("/test")
 *   public ResponseEntity<?> test(@CurrentSecurityContext(expression = "authentication?.name") String email)
 *
 * The "email" parameter is automatically filled from the SecurityContext by Spring!
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * COOKIE vs AUTHORIZATION HEADER (Which is better?)
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * AUTHORIZATION HEADER (Bearer Token):
 *   Advantages: Works everywhere, any client can use it
 *   Disadvantages: Must be sent by client in every request
 *   Use case: Mobile apps, frontend frameworks, any API client
 *
 * HTTP-ONLY COOKIE:
 *   Advantages: Automatic (browser sends it), protected from XSS
 *   Disadvantages: Only works for browsers
 *   Use case: Web applications, safer against JavaScript attacks
 *
 * OUR APPROACH:
 *   → Send JWT in BOTH places from login endpoint
 *   → This filter checks BOTH (header first, then cookie)
 *   → Gives flexibility for different clients
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * FILTER ORDER IN SPRING SECURITY
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * In SecurityConfig, we add this filter BEFORE UsernamePasswordAuthenticationFilter:
 *   .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
 *
 * Why? Because:
 *   • JwtRequestFilter must run first to check token
 *   • UsernamePasswordAuthenticationFilter is for form/session login (we don't use it)
 *   • By running first, JWT validation happens before other login methods
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * TROUBLESHOOTING CHECKLIST
 * ═══════════════════════════════════════════════════════════════════════════════════
 *
 * "isAuthenticated returns false even after login?"
 *   1. Check if browser is sending the JWT cookie: Open DevTools → Application → Cookies
 *   2. Check if the "jwt" cookie exists and has a value
 *   3. If using frontend, ensure you have: fetch(url, { credentials: 'include' })
 *   4. Check AuthController.login - does it set the Set-Cookie header correctly?
 *
 * "Getting 401 Unauthorized on protected endpoints?"
 *   1. Did you login first? POST /api/v1.1/auth/login
 *   2. Is the token being sent? Check Authorization header or cookie
 *   3. Is the token expired? Tokens are valid for 10 hours
 *   4. Check the token format: Should be "Bearer <token>" in header
 *
 * "Cookie not being set?"
 *   1. Check if SameSite is "Lax" (in AuthController)
 *   2. If frontend is cross-origin: Need SameSite="None" and secure=true
 *   3. Ensure CORS has allowCredentials=true (check SecurityConfig)
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    private final AppUserDetaisService appUserDetaisService;
    private final JwtUtil jwtUtil;
    private static final List<String> PUBLIC_URLS = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/signup",
            "/auth/verify",
            "/auth/send-reset-otp",
            "/auth/reset-password",
            "/auth/logout",
            "/auth/request-password-reset",
            "/auth/resend-verification",
            "/test"
    );

    /**
     * Core filtering logic executed for each HTTP request.
     *
     * @param request  incoming HTTP request
     * @param response outgoing HTTP response
     * @param filterChain remaining filters to execute
     * @throws ServletException if servlet-related error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        // 1. Allow public endpoints to pass through without JWT processing
        if (PUBLIC_URLS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = null;
        String email;

        // 2. Prefer Authorization header: Authorization: Bearer <token>
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // remove "Bearer " prefix
        }
        // 3. Fallback to HTTP-only cookie (helps with XSS protection)
        if (jwt == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("jwt")) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // 4. Validate and set authentication if token present
        if (jwt != null) {
            email = jwtUtil.extractEmail(jwt);
            // Ensure we don't override existing authentication
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = appUserDetaisService.loadUserByUsername(email);
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }

        // 5. Continue processing chain (either authenticated or anonymous)
        filterChain.doFilter(request, response);
    }
}
