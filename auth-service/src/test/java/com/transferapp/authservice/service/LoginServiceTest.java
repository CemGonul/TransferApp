package com.transferapp.authservice.service;

import com.transferapp.authservice.dto.LoginRequest;
import com.transferapp.authservice.entity.UserAuth;
import com.transferapp.authservice.repository.UserAuthRepository;
import com.transferapp.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private JwtService jwtService;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(authenticationManager, userAuthRepository, jwtService);
    }

    @Test
    void loginNormalizesEmailAuthenticatesAndReturnsToken() {
        LoginRequest request = new LoginRequest("  USER@Example.COM  ", "strongPassword12");
        UserAuth user = new UserAuth("user@example.com", "hashed-password");
        when(userAuthRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        var response = loginService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        var authentication = authenticationCaptor.getValue();

        assertAll(
                () -> assertEquals("user@example.com", authentication.getPrincipal()),
                () -> assertEquals("strongPassword12", authentication.getCredentials()),
                () -> assertEquals("signed.jwt.token", response.accessToken()),
                () -> assertEquals("Bearer", response.tokenType()),
                () -> assertEquals(900L, response.expiresIn())
        );
    }

    @Test
    void loginStopsWhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("user@example.com", "wrongPassword12");
        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> loginService.login(request));

        verify(userAuthRepository, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
        verifyNoInteractions(jwtService);
    }
}
