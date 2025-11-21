package com.arpon007.FullStackAuth.config;

import com.arpon007.FullStackAuth.Entity.Role.RoleEntity;
import com.arpon007.FullStackAuth.Entity.Role.RoleName;
import com.arpon007.FullStackAuth.Entity.UserEntity;
import com.arpon007.FullStackAuth.repository.RoleRepository;
import com.arpon007.FullStackAuth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInit implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${adminEmail:}")
    private String adminEmail;

    @Value("${adminPass:}")
    private String adminPassword;

    @Value("${ADMIN_NAME:Administrator}")
    private String adminName;

    @Override
    public void run(String... args) {
        ensureDefaultRoles();
        seedDefaultAdmin();
    }

    private void ensureDefaultRoles() {
        if (!roleRepository.existsByName(RoleName.USER)) {
            roleRepository.save(RoleEntity.builder().name(RoleName.USER).build());
            log.info("Seeded default role: USER");
        }
        if (!roleRepository.existsByName(RoleName.ADMIN)) {
            roleRepository.save(RoleEntity.builder().name(RoleName.ADMIN).build());
            log.info("Seeded default role: ADMIN");
        }
    }

    private void seedDefaultAdmin() {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("adminEmail or adminPass not set in environment. Skipping admin seeding.");
            return;
        }

        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                existing -> log.info("Default admin already exists: {}", adminEmail),
                () -> {
                    var adminRole = roleRepository.findByName(RoleName.ADMIN).orElseGet(() -> roleRepository.save(RoleEntity.builder().name(RoleName.ADMIN).build()));
                    var roles = new HashSet<RoleEntity>();
                    roles.add(adminRole);

                    UserEntity admin = UserEntity.builder()
                            .email(adminEmail)
                            .userId(UUID.randomUUID().toString())
                            .name(adminName)
                            .password(passwordEncoder.encode(adminPassword))
                            .isAccountVerified(true)
                            .resetOtpExpireAt(0L)
                            .verifyOtp(null)
                            .verifyOtpExpireAt(0L)
                            .resetOtp(null)
                            .roles(roles)
                            .build();

                    userRepository.save(admin);
                    log.info("Seeded default admin user: {}", adminEmail);
                }
        );
    }
}