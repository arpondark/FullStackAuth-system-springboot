package com.arpon007.FullStackAuth.repository;

import com.arpon007.FullStackAuth.Entity.Role.RoleEntity;
import com.arpon007.FullStackAuth.Entity.Role.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(RoleName name);
    boolean existsByName(RoleName name);
}