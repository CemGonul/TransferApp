package com.transferapp.userservice.service;

import com.transferapp.userservice.dto.UpdateUserProfileRequest;
import com.transferapp.userservice.entity.UserProfile;
import com.transferapp.userservice.enums.ProfileStatus;
import com.transferapp.userservice.exception.UserProfileAlreadyExistsException;
import com.transferapp.userservice.exception.UserProfileNotFoundException;
import com.transferapp.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userProfileRepository);
    }

    @Test
    void createProfileCreatesIncompleteProfile() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(false);

        userProfileService.createProfile(userId);

        ArgumentCaptor<UserProfile> profileCaptor =
                ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();

        assertAll(
                () -> assertEquals(userId, savedProfile.getUserId()),
                () -> assertEquals(ProfileStatus.INCOMPLETE, savedProfile.getStatus()),
                () -> assertNull(savedProfile.getFirstName()),
                () -> assertNull(savedProfile.getLastName()),
                () -> assertNull(savedProfile.getPhoneNumber()),
                () -> assertNull(savedProfile.getCountryCode()),
                () -> assertNotNull(savedProfile.getCreatedAt()),
                () -> assertEquals(savedProfile.getCreatedAt(), savedProfile.getUpdatedAt())
        );
    }

    @Test
    void createProfileRejectsExistingProfile() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.existsById(userId)).thenReturn(true);

        UserProfileAlreadyExistsException exception = assertThrows(
                UserProfileAlreadyExistsException.class,
                () -> userProfileService.createProfile(userId)
        );

        assertTrue(exception.getMessage().contains(userId.toString()));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void getCurrentProfileReturnsResponse() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId);
        profile.updatePersonalInformation("Ali", "Yilmaz", "+48123456789", "PL");
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));

        var response = userProfileService.getCurrentProfile(userId);

        assertAll(
                () -> assertEquals(userId, response.userId()),
                () -> assertEquals("Ali", response.firstName()),
                () -> assertEquals("Yilmaz", response.lastName()),
                () -> assertEquals("+48123456789", response.phoneNumber()),
                () -> assertEquals("PL", response.countryCode()),
                () -> assertEquals(ProfileStatus.ACTIVE, response.status()),
                () -> assertEquals(profile.getCreatedAt(), response.createdAt()),
                () -> assertEquals(profile.getUpdatedAt(), response.updatedAt())
        );
    }

    @Test
    void getCurrentProfileThrowsWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        UserProfileNotFoundException exception = assertThrows(
                UserProfileNotFoundException.class,
                () -> userProfileService.getCurrentProfile(userId)
        );

        assertTrue(exception.getMessage().contains(userId.toString()));
    }

    @Test
    void updateCurrentProfileNormalizesAndReturnsProfile() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId);
        Instant initialUpdatedAt = profile.getUpdatedAt();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "  Ali  ",
                "  Yilmaz  ",
                "  +48123456789  ",
                " pl "
        );

        var response = userProfileService.updateCurrentProfile(userId, request);

        assertAll(
                () -> assertEquals("Ali", response.firstName()),
                () -> assertEquals("Yilmaz", response.lastName()),
                () -> assertEquals("+48123456789", response.phoneNumber()),
                () -> assertEquals("PL", response.countryCode()),
                () -> assertEquals(ProfileStatus.ACTIVE, response.status()),
                () -> assertFalse(response.updatedAt().isBefore(initialUpdatedAt))
        );
    }

    @Test
    void updateCurrentProfileConvertsBlankPhoneToNullWithoutSavingExplicitly() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId);
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Ali",
                "Yilmaz",
                "   ",
                "PL"
        );

        var response = userProfileService.updateCurrentProfile(userId, request);

        assertNull(response.phoneNumber());
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void updateCurrentProfileThrowsWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Ali",
                "Yilmaz",
                null,
                "PL"
        );

        UserProfileNotFoundException exception = assertThrows(
                UserProfileNotFoundException.class,
                () -> userProfileService.updateCurrentProfile(userId, request)
        );

        assertTrue(exception.getMessage().contains(userId.toString()));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }
}
