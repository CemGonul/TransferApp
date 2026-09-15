package com.transferapp.authservice.service;


import com.transferapp.authservice.dto.LoginRequest;
import com.transferapp.authservice.dto.LoginResponse;
import com.transferapp.authservice.entity.UserAuth;
import com.transferapp.authservice.repository.UserAuthRepository;
import com.transferapp.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final UserAuthRepository userAuthRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        String normalizedEmail = normalizeEmail(request.email());

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        request.password()
                );

        authenticationManager.authenticate(authenticationToken);

        UserAuth user = userAuthRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token= jwtService.generateToken(user);
        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
