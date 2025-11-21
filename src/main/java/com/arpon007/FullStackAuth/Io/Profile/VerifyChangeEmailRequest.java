package com.arpon007.FullStackAuth.Io.Profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyChangeEmailRequest {
    @NotBlank(message = "OTP is required")
    private String otp;
}
