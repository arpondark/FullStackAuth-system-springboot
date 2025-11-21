package com.arpon007.FullStackAuth.Io.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthRequest represents the payload sent by the client to log in.
 *
 * Fields:
 * - email: user's email (used as the username/subject in JWT)
 * - password: user's raw password (will be authenticated and never returned)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthRequest {
    private String email;
    private String password;
}
