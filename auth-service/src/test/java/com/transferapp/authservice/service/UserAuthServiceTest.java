package com.transferapp.authservice.service;

import com.transferapp.authservice.dto.RegisterRequest;
import com.transferapp.authservice.entity.UserAuth;
import com.transferapp.authservice.enums.Role;
import com.transferapp.authservice.exception.DuplicateEmailException;
import com.transferapp.authservice.repository.UserAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private UserAuthRepository userAuthRepository;

    private PasswordEncoder passwordEncoder;
    private UserAuthService userAuthService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userAuthService = new UserAuthService(userAuthRepository, passwordEncoder);
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndSavesUser() {
        RegisterRequest request = new RegisterRequest(
                "  USER@Example.COM  ",
                "strongPassword12"
        );
        when(userAuthRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userAuthRepository.save(any(UserAuth.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = userAuthService.register(request);

        ArgumentCaptor<UserAuth> userCaptor = ArgumentCaptor.forClass(UserAuth.class);
        verify(userAuthRepository).save(userCaptor.capture());
        UserAuth savedUser = userCaptor.getValue();

        assertAll(
                () -> assertEquals("user@example.com", savedUser.getEmail()),
                () -> assertFalse(savedUser.getHashedPassword().equals(request.password())),
                () -> assertTrue(passwordEncoder.matches(request.password(), savedUser.getHashedPassword())),
                () -> assertEquals(Role.USER, savedUser.getRole()),
                () -> assertTrue(savedUser.isEnabled()),
                () -> assertNotNull(savedUser.getCreatedAt()),
                () -> assertEquals("user@example.com", response.email()),
                () -> assertEquals("Registration successful", response.message())
        );
    }

    @Test
    void registerRejectsExistingEmailBeforeHashingOrSaving() {
        RegisterRequest request = new RegisterRequest(
                "Existing@Example.com",
                "strongPassword12"
        );
        when(userAuthRepository.existsByEmail("existing@example.com")).thenReturn(true);

        DuplicateEmailException exception = assertThrows(
                DuplicateEmailException.class,
                () -> userAuthService.register(request)
        );

        assertEquals("Email already in use", exception.getMessage());
        verify(userAuthRepository, never()).save(any(UserAuth.class));
    }

    @Test
    void registerDoesNotMislabelOtherDatabaseIntegrityErrors() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com",
                "strongPassword12"
        );
        when(userAuthRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userAuthRepository.save(any(UserAuth.class)))
                .thenThrow(new DataIntegrityViolationException("Database constraint failed"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userAuthService.register(request)
        );
    }
}
