package com.arpon007.FullStackAuth.Io.Profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyChangePasswordRequest {
    @NotBlank(message = "OTP is required")
    private String otp;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
