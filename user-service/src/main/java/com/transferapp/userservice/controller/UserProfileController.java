package com.transferapp.userservice.controller;


import com.transferapp.userservice.dto.UpdateUserProfileRequest;
import com.transferapp.userservice.dto.UserProfileResponse;
import com.transferapp.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserProfileController {
    private final UserProfileService userProfileService;


    @GetMapping("/me")
    public UserProfileResponse getCurrentUser(
            @AuthenticationPrincipal Jwt jwt){
        UUID userId =UUID.fromString(jwt.getSubject());
        return userProfileService.getCurrentProfile(userId);
    }

    @PutMapping("/me")
    public UserProfileResponse updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request){
        UUID userId =UUID.fromString(jwt.getSubject());
        return userProfileService.updateCurrentProfile(userId,request);

    }
}
