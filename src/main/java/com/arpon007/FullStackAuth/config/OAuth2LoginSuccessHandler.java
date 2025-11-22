package com.arpon007.FullStackAuth.config;

import com.arpon007.FullStackAuth.Entity.AuthProvider;
import com.arpon007.FullStackAuth.Entity.Role.RoleEntity;
import com.arpon007.FullStackAuth.Entity.Role.RoleName;
import com.arpon007.FullStackAuth.Entity.UserEntity;
import com.arpon007.FullStackAuth.Util.JwtUtil;
import com.arpon007.FullStackAuth.repository.RoleRepository;
import com.arpon007.FullStackAuth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getName(); // Google sub

        Optional<UserEntity> userOptional = userRepository.findByEmail(email);
        UserEntity user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update existing user details
            user.setName(name); // Update name from Google
            user.setIsAccountVerified(true); // Google emails are verified
            user.setProviderId(providerId);
            user.setAuthProvider(AuthProvider.GOOGLE);
            userRepository.save(user);
        } else {
            // Create new user
            RoleEntity userRole = roleRepository.findByName(RoleName.USER).orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            Set<RoleEntity> roles = new HashSet<>();
            roles.add(userRole);

            user = UserEntity.builder()
                    .email(email)
                    .name(name)
                    .userId(UUID.randomUUID().toString())
                    .isAccountVerified(true) // Google verified
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .roles(roles)
                    .password("") // No password for OAuth users
                    .build();
            userRepository.save(user);
        }

        // Generate JWT
        // We need UserDetails for JwtUtil.generateToken
        // We can construct a simple UserDetails object
        UserDetails userDetails = new User(
                user.getEmail(),
                user.getPassword() != null ? user.getPassword() : "",
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .collect(Collectors.toList())
        );

        String token = jwtUtil.generateToken(userDetails);

        // Redirect to frontend
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        // Create HTTP-only cookie
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .path("/")
                .maxAge(java.time.Duration.ofDays(1))
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
