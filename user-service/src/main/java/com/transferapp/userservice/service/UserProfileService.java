package com.transferapp.userservice.service;


import com.transferapp.userservice.dto.UpdateUserProfileRequest;
import com.transferapp.userservice.dto.UserProfileResponse;
import com.transferapp.userservice.entity.UserProfile;
import com.transferapp.userservice.exception.UserProfileAlreadyExistsException;
import com.transferapp.userservice.exception.UserProfileNotFoundException;
import com.transferapp.userservice.repository.UserProfileRepository;

import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    @Transactional
    public boolean ensureProfileExists(UUID userId){
        if(userProfileRepository.existsById(userId)){
            return false;
        }
        UserProfile profile=new UserProfile(userId);
        userProfileRepository.save(profile);
        return true;

    }
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile(UUID userId){
        UserProfile profile=findProfile(userId);
        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(
            UUID userId,
            UpdateUserProfileRequest request) {

        UserProfile profile = findProfile(userId);

        UpdateUserProfileRequest normalizedRequest =
                normalizeRequest(request);

        profile.updatePersonalInformation(
                normalizedRequest.firstName(),
                normalizedRequest.lastName(),
                normalizedRequest.phoneNumber(),
                normalizedRequest.countryCode()
        );
        return toResponse(profile);
    }

    private UserProfileResponse toResponse(UserProfile profile){
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhoneNumber(),
                profile.getCountryCode(),
                profile.getStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private UpdateUserProfileRequest normalizeRequest(
            UpdateUserProfileRequest request) {

        String phoneNumber = request.phoneNumber();

        if (phoneNumber != null) {
            phoneNumber = phoneNumber.trim();

            if (phoneNumber.isBlank()) {
                phoneNumber = null;
            }
        }

        return new UpdateUserProfileRequest(
                request.firstName().trim(),
                request.lastName().trim(),
                phoneNumber,
                request.countryCode().trim().toUpperCase(Locale.ROOT)
        );
    }

    private UserProfile findProfile(UUID userId){
        return userProfileRepository
                .findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(
                        "User profile not found: " + userId
                ));
    }
}
