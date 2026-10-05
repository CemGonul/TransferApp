package com.transferapp.authservice.service;

import com.transferapp.authservice.client.UserProfileClient;
import com.transferapp.authservice.dto.RegisterRequest;
import com.transferapp.authservice.dto.RegisterResponse;
import com.transferapp.authservice.entity.UserAuth;
import com.transferapp.authservice.exception.DuplicateEmailException;
import com.transferapp.authservice.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

        UserAuth user = userAuthRepository.findByEmail(normalizedEmail)
                .map(existing -> getPendingRegistration(existing, request.password()))
                .orElseGet(() -> createPendingRegistration(normalizedEmail, request.password()));

        userProfileClient.ensureProfileExists(user.getId());

        if (userAuthRepository.completeRegistration(user.getId()) != 1) {
            throw new IllegalStateException("Registration user was not found");
        }

        return new RegisterResponse(
                user.getEmail(),
                "User registered successfully"
        );
    }

    private UserAuth createPendingRegistration(String email, String password) {

        String hashedPassword =
                passwordEncoder.encode(password);

        UserAuth user = new UserAuth(
                email,
                hashedPassword
        );

        try {
            return userAuthRepository.save(user);
        } catch (DataIntegrityViolationException exception) {
            // A concurrent registration may have committed the same email first.
            UserAuth existing = userAuthRepository.findByEmail(email)
                    .orElseThrow(() -> exception);
            return getPendingRegistration(existing, password);
        }
    }

    private UserAuth getPendingRegistration(UserAuth user, String password) {
        if (user.isRegistrationCompleted()
                || !user.isEnabled()
                || !passwordEncoder.matches(password, user.getHashedPassword())) {
            throw new DuplicateEmailException("Email is already registered");
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
