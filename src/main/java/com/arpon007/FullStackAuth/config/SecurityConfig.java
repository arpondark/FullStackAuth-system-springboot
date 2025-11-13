package com.arpon007.FullStackAuth.config;


import com.arpon007.FullStackAuth.Filter.JwtRequestFilter;
import com.arpon007.FullStackAuth.Service.AppUserDetaisService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig is a configuration class that sets up Spring Security for the Full Stack Auth application.
 * It configures:
 * - CORS (Cross-Origin Resource Sharing) to allow frontend requests from different origins
 * - CSRF protection (disabled for stateless API)
 * - Authorization rules for different endpoints
 * - Session management (stateless for JWT/token-based authentication)
 * - Logout configuration
 * - Password encoding using BCrypt
 */
@Configuration
@RequiredArgsConstructor
@Data
public class SecurityConfig {
    private final AppUserDetaisService appUserDetaisService;
    private final JwtRequestFilter jwtRequestFilter;
    private final CustomAuhtenticationEntryPoint customAuhtenticationEntryPoint;

    /**
     * filterChain() configures the security filter chain for HTTP requests.
     * <p>
     * How JWT is applied:
     * - We register JwtRequestFilter before Spring's UsernamePasswordAuthenticationFilter, so for every request
     * our filter extracts and validates the token and sets the SecurityContext when valid.
     * - Public vs Protected routes: Authorization rules permit /auth/** (which becomes /api/v1.1/auth/** due to context-path).
     *   All other endpoints require an authenticated SecurityContext; JwtRequestFilter is responsible for building it from the JWT.
     * <p>
     * Configuration Details:
     * - CORS: Enables CORS with default settings (uses corsConfigurationSource() bean)
     * - CSRF: Disabled because this is a stateless API (uses JWT/tokens, not sessions)
     * - Session Management: STATELESS (we don't use server sessions; JWT carries the user identity)
     * - Logout: Disabled (you can implement cookie/token clearing in your controller if needed)
     *
     * Note: With server.servlet.context-path=/api/v1.1, all paths below are relative to the context path.
     * So "/auth/**" translates to /api/v1.1/auth/** when accessed from outside.
     *
     * @param http the HttpSecurity object to configure
     * @return SecurityFilterChain the configured filter chain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register", "/auth/send-reset-otp", "/auth/reset-password", "/auth/logout").permitAll()
                        .requestMatchers("/test").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuhtenticationEntryPoint));

        return http.build();

    }

    /**
     * passwordEncoder() creates and provides a PasswordEncoder bean.
     * <p>
     * Purpose:
     * - Encodes user passwords using BCrypt algorithm for secure storage in database
     * - BCrypt automatically handles salt generation and hashing
     * - Used during user registration and authentication
     *
     * @return PasswordEncoder instance (BCryptPasswordEncoder)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * corsConfigurationSource() configures CORS settings for the application.
     * <p>
     * CORS Configuration:
     * - Allowed Origins: localhost:3000, localhost:5173, 127.0.0.1:3000, 127.0.0.1:5173
     * (Supports both Vite and React development servers)
     * - Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH (all standard REST operations)
     * - Allowed Headers: Authorization, Content-Type (for JWT tokens and JSON data)
     * - Allow Credentials: true (allows cookies/tokens in cross-origin requests)
     * - Max Age: 3600 seconds (1 hour cache for preflight requests)
     * <p>
     * Purpose:
     * - Allows frontend applications running on different ports/domains to make requests to this backend
     * - Handles browser preflight requests (OPTIONS) automatically
     * - Enables JWT token transmission in Authorization headers
     *
     * @return CorsConfigurationSource configured for frontend integration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000", "http://127.0.0.1:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(appUserDetaisService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }
}
