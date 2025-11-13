package com.arpon007.FullStackAuth.Io;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;
    @Email(message = "Enter a valid email address")
    @NotNull(message = "Email cannot be empty")
    private String email;
    @Size(min = 6,message = "Password must be at least 6 characters")
    private String password;
}
