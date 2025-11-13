package com.arpon007.FullStackAuth.Service;

import com.arpon007.FullStackAuth.Entity.UserEntity;
import com.arpon007.FullStackAuth.Io.ProfileRequest;
import com.arpon007.FullStackAuth.Io.ProfileResponse;
import com.arpon007.FullStackAuth.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Data
@AllArgsConstructor
public class ProfileServiceImpl implements ProfileService {


    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ProfileResponse createProfile(ProfileRequest request) {
        UserEntity newProfile = convertToUserEntity(request);
        if (!userRepository.existsByEmail(request.getEmail())) {
            newProfile = userRepository.save(newProfile);
            return convertToUserResponse(newProfile);
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }

    @Override
    public ProfileResponse getProfile(String email) {
        UserEntity existUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return convertToUserResponse(existUser);
    }

    @Override
    public void sendResetOpt(String email) {
        UserEntity existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        //genereate otp
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));

        //expire time
        long expireTime = System.currentTimeMillis() + (1000 * 15 * 60); //15min
        existingUser.setResetOtpExpireAt(expireTime);
        existingUser.setResetOtp(otp);
        userRepository.save(existingUser);
        try {
            emailService.sendResetOtpEmail(email, otp); //reset otp email
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ProfileResponse convertToUserResponse(UserEntity newProfile) {
        return ProfileResponse.builder()
                .userId(newProfile.getUserId())
                .name(newProfile.getName())
                .email(newProfile.getEmail())
                .isAccountVerified(newProfile.getIsAccountVerified())
                .build();

    }

    private UserEntity convertToUserEntity(ProfileRequest request) {
        return UserEntity.builder()
                .email(request.getEmail())
                .userId(UUID.randomUUID().toString())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .isAccountVerified(false)
                .resetOtpExpireAt(0L)
                .verifyOtp(null)
                .verifyOtpExpireAt(0L)
                .resetOtp(null)
                .build();

    }
}
