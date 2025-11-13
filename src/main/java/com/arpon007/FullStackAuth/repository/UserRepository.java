package com.arpon007.FullStackAuth.repository;

import com.arpon007.FullStackAuth.Entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * UserRepository provides database access methods for UserEntity.
 *
 * Methods used in authentication:
 * - findByEmail(String): fetches a user by email for login/JWT validation.
 * - existsByEmail(String): checks if a user already exists during registration.
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Boolean existsByEmail(String email);
}
