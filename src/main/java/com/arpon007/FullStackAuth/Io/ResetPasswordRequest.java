package com.arpon007.FullStackAuth.Io;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {
    @NotBlank(message = "New password cannot be blank")
    private String newPassword;
    @NotBlank(message = "OTP cannot be blank")
    private String otp;
    @NotBlank(message = "Email cannot be blank")
    private String email;
}
