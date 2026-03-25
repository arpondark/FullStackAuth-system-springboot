package com.arpon007.FullStackAuth.Controller;

import com.arpon007.FullStackAuth.Io.Admin.AdminCreateUserRequest;
import com.arpon007.FullStackAuth.Io.Profile.ProfileResponse;
import com.arpon007.FullStackAuth.Service.Profile.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ProfileService profileService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> addAdminOrUser(@Valid @RequestBody AdminCreateUserRequest request) {
        ProfileResponse newProfile = profileService.createUserByAdmin(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User created successfully");
        response.put("data", newProfile);

        return ResponseEntity.status(201).body(response);
    }
}
