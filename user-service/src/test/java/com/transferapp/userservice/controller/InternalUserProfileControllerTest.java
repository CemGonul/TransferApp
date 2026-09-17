package com.transferapp.userservice.controller;

import com.transferapp.userservice.config.SecurityConfig;
import com.transferapp.userservice.exception.GlobalExceptionHandler;
import com.transferapp.userservice.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalUserProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InternalUserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void requiresAuthentication() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/internal/user-profiles/{userId}", userId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userProfileService);
    }

    @Test
    void rejectsUserRole() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/internal/user-profiles/{userId}", userId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userProfileService);
    }

    @Test
    void returnsCreatedWhenServiceCreatesProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userProfileService.ensureProfileExists(userId)).thenReturn(true);

        mockMvc.perform(put("/internal/user-profiles/{userId}", userId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_SERVICE"))))
                .andExpect(status().isCreated());

        verify(userProfileService).ensureProfileExists(userId);
    }

    @Test
    void returnsNoContentWhenProfileAlreadyExists() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userProfileService.ensureProfileExists(userId)).thenReturn(false);

        mockMvc.perform(put("/internal/user-profiles/{userId}", userId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_SERVICE"))))
                .andExpect(status().isNoContent());

        verify(userProfileService).ensureProfileExists(userId);
    }
}
