package com.arpon007.FullStackAuth.Entity;

import com.arpon007.FullStackAuth.Entity.Role.RoleEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * UserEntity maps to the tbl_users table and stores authentication-related data.
 *
 * Important fields for authentication/JWT:
 * - email: used as the unique username/subject inside JWT tokens.
 * - password: BCrypt-hashed password (never store raw passwords).
 * - userId: public UUID-style identifier (can be exposed safely to clients instead of internal id).
 * - isAccountVerified: can be leveraged to restrict login until email/OTP verification.
 *
 * OTP / Verification workflow fields (extend as needed):
 * - verifyOtp & verifyOtpExpireAt: for account/email verification logic.
 * - resetOtp & resetOtpExpireAt: for password reset flows.
 *
 * Timestamps:
 * - createdAt, updatedAt managed automatically by Hibernate for auditing.
 */
@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tbl_users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String userId;
    private String name;
    private String password;
    @Column(unique = true)
    private String email;
    private String verifyOtp;
    private Long verifyOtpExpireAt;
    private String resetOtp;
    private Long resetOtpExpireAt;
    private Boolean isAccountVerified;
    private String pendingEmail;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tbl_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

}
