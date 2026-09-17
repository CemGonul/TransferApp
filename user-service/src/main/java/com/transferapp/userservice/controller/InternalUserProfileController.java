package com.transferapp.userservice.controller;


import com.transferapp.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/user-profiles")
public class InternalUserProfileController {
    private final UserProfileService userProfileService;

    @PutMapping("/{userId}")
    public ResponseEntity<Void> ensureProfileExists(
        @PathVariable UUID userId){

        if(userProfileService.ensureProfileExists(userId)){
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        return ResponseEntity.noContent().build();
    }
}
