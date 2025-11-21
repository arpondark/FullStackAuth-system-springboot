package com.arpon007.FullStackAuth.Io.Profile;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(min = 2, max = 100)
    private String name;
}