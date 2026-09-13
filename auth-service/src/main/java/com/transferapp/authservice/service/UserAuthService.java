package com.transferapp.authservice.service;

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

    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userAuthRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email already in use");
        }
        String hashedPassword = passwordEncoder.encode(request.password());

        UserAuth user = new UserAuth(
                normalizedEmail,
                hashedPassword
        );
        UserAuth savedUser = userAuthRepository.save(user);

        return new RegisterResponse(
                savedUser.getEmail(),
                "Registration successful"
        );

    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
