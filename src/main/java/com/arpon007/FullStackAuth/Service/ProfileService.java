package com.arpon007.FullStackAuth.Service;

import com.arpon007.FullStackAuth.Io.ProfileRequest;
import com.arpon007.FullStackAuth.Io.ProfileResponse;
import org.springframework.context.annotation.Profile;

public interface ProfileService {
    ProfileResponse createProfile(ProfileRequest request);

    ProfileResponse getProfile(String email);

    void sendResetOpt(String email);
}
