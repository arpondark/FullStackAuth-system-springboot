package com.arpon007.FullStackAuth.Io.Auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AuthResponse is returned after a successful login.
 *
 * Fields:
 * - email: the authenticated user's email (same as subject in JWT)
 * - token: the signed JWT string the client should store (or is also set as httpOnly cookie)
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String email;
    private String token;
}
