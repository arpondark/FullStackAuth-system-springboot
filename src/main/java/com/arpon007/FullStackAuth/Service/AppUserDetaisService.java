package com.arpon007.FullStackAuth.Service;

import com.arpon007.FullStackAuth.Entity.UserEntity;
import com.arpon007.FullStackAuth.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * AppUserDetaisService bridges the database (UserEntity) with Spring Security.
 *
 * Purpose in JWT flow:
 * - When a JWT is received, the subject (email) is extracted.
 * - Spring calls loadUserByUsername(email) to fetch password + authorities (roles) for validation or context.
 * - We return a Spring Security User object built from our UserEntity.
 *
 * Notes:
 * - Currently returns an empty list of authorities; extend to include roles/permissions if needed.
 * - Throws UsernameNotFoundException if the email is not in the database (invalid token or stale user).
 */
@Service
@Data
@RequiredArgsConstructor
public class AppUserDetaisService implements UserDetailsService {
    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new User(existingUser.getEmail(), existingUser.getPassword(), new ArrayList<>());
    }
}
