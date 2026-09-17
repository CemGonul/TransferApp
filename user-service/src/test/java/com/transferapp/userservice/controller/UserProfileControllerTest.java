package com.transferapp.userservice.controller;

import com.transferapp.userservice.config.SecurityConfig;
import com.transferapp.userservice.dto.UpdateUserProfileRequest;
import com.transferapp.userservice.dto.UserProfileResponse;
import com.transferapp.userservice.enums.ProfileStatus;
import com.transferapp.userservice.exception.GlobalExceptionHandler;
import com.transferapp.userservice.exception.UserProfileNotFoundException;
import com.transferapp.userservice.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getCurrentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userProfileService);
    }

    @Test
    void getCurrentUserUsesSubjectFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = profileResponse(userId);
        when(userProfileService.getCurrentProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/users/me")
                        .with(jwt().jwt(token -> token
                                .subject(userId.toString())
                                .claim("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ali"))
                .andExpect(jsonPath("$.lastName").value("Yilmaz"))
                .andExpect(jsonPath("$.countryCode").value("PL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userProfileService).getCurrentProfile(userId);
    }

    @Test
    void updateCurrentUserUsesSubjectFromJwt() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "Ali",
                "Yilmaz",
                "+48123456789",
                "pl"
        );
        when(userProfileService.updateCurrentProfile(userId, request))
                .thenReturn(profileResponse(userId));

        mockMvc.perform(put("/users/me")
                        .with(jwt().jwt(token -> token
                                .subject(userId.toString())
                                .claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ali",
                                  "lastName": "Yilmaz",
                                  "phoneNumber": "+48123456789",
                                  "countryCode": "pl"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.countryCode").value("PL"));

        verify(userProfileService).updateCurrentProfile(userId, request);
    }

    @Test
    void updateCurrentUserRejectsInvalidCountryCode() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/users/me")
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ali",
                                  "lastName": "Yilmaz",
                                  "phoneNumber": "+48123456789",
                                  "countryCode": "POL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(userProfileService);
    }

    @Test
    void updateCurrentUserRejectsMalformedJson() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/users/me")
                        .with(jwt().jwt(token -> token.subject(userId.toString())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_BODY"));

        verifyNoInteractions(userProfileService);
    }

    @Test
    void getCurrentUserReturnsNotFoundWhenProfileDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userProfileService.getCurrentProfile(userId))
                .thenThrow(new UserProfileNotFoundException("User profile not found: " + userId));

        mockMvc.perform(get("/users/me")
                        .with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_PROFILE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/users/me"));
    }

    private UserProfileResponse profileResponse(UUID userId) {
        Instant now = Instant.now();
        return new UserProfileResponse(
                userId,
                "Ali",
                "Yilmaz",
                "+48123456789",
                "PL",
                ProfileStatus.ACTIVE,
                now.minusSeconds(60),
                now
        );
    }
}
