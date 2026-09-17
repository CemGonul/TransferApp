package com.transferapp.authservice.service;

import com.transferapp.authservice.client.UserProfileClient;
import com.transferapp.authservice.dto.RegisterRequest;
import com.transferapp.authservice.entity.UserAuth;
import com.transferapp.authservice.enums.Role;
import com.transferapp.authservice.exception.DuplicateEmailException;
import com.transferapp.authservice.exception.UserProfileProvisioningException;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private UserProfileClient userProfileClient;

    private PasswordEncoder passwordEncoder;
    private UserAuthService userAuthService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userAuthService = new UserAuthService(
                userAuthRepository,
                passwordEncoder,
                userProfileClient
        );
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndSavesUser() {
        RegisterRequest request = new RegisterRequest(
                "  USER@Example.COM  ",
                "strongPassword12"
        );
        UUID userId = UUID.randomUUID();
        when(userAuthRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userAuthRepository.save(any(UserAuth.class)))
                .thenAnswer(invocation -> {
                    UserAuth savedUser = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedUser, "id", userId);
                    return savedUser;
                });

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
                () -> assertEquals("User registered successfully", response.message())
        );
        verify(userProfileClient).ensureProfileExists(userId);
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

        assertEquals("Email is already registered", exception.getMessage());
        verify(userAuthRepository, never()).save(any(UserAuth.class));
        verifyNoInteractions(userProfileClient);
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
        verifyNoInteractions(userProfileClient);
    }

    @Test
    void registerPropagatesProvisioningFailureAfterSavingAuthUser() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com",
                "strongPassword12"
        );
        UUID userId = UUID.randomUUID();
        when(userAuthRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userAuthRepository.save(any(UserAuth.class)))
                .thenAnswer(invocation -> {
                    UserAuth savedUser = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedUser, "id", userId);
                    return savedUser;
                });
        UserProfileProvisioningException failure =
                new UserProfileProvisioningException("User-service is unreachable");
        org.mockito.Mockito.doThrow(failure)
                .when(userProfileClient)
                .ensureProfileExists(userId);

        UserProfileProvisioningException thrown = assertThrows(
                UserProfileProvisioningException.class,
                () -> userAuthService.register(request)
        );

        assertEquals(failure, thrown);
        verify(userAuthRepository).save(any(UserAuth.class));
        verify(userProfileClient).ensureProfileExists(userId);
    }
}
