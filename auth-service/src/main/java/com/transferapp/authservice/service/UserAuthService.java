package com.transferapp.authservice.service;

import com.transferapp.authservice.client.UserProfileClient;
import com.transferapp.authservice.dto.RegisterRequest;
import com.transferapp.authservice.dto.RegisterResponse;
import com.transferapp.authservice.entity.UserAuth;
import com.transferapp.authservice.exception.DuplicateEmailException;
import com.transferapp.authservice.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserAuthService {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProfileClient userProfileClient;

    public RegisterResponse register(RegisterRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        if (userAuthRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email is already registered");
        }

        String hashedPassword =
                passwordEncoder.encode(request.password());

        UserAuth user = new UserAuth(
                normalizedEmail,
                hashedPassword
        );

        UserAuth savedUser = userAuthRepository.save(user);

        userProfileClient.ensureProfileExists(savedUser.getId());

        return new RegisterResponse(
                savedUser.getEmail(),
                "User registered successfully"
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
