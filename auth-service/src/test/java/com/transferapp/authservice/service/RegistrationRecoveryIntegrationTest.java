package com.transferapp.authservice.service;

import com.transferapp.authservice.client.UserProfileClient;
import com.transferapp.authservice.dto.LoginRequest;
import com.transferapp.authservice.dto.RegisterRequest;
import com.transferapp.authservice.entity.UserAuth;
import com.transferapp.authservice.exception.DuplicateEmailException;
import com.transferapp.authservice.exception.UserProfileProvisioningException;
import com.transferapp.authservice.repository.UserAuthRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegistrationRecoveryIntegrationTest {

    @Autowired
    private UserAuthService userAuthService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileClient userProfileClient;

    @Test
    void retriesPendingRegistrationAfterProfileResponseIsLostAndThenAllowsLogin() {
        String email = "registration-recovery-" + UUID.randomUUID() + "@example.com";
        String password = "strongPassword12";
        RegisterRequest request = new RegisterRequest(email, password);
        Set<UUID> provisionedProfiles = new HashSet<>();
        doAnswer(invocation -> {
            provisionedProfiles.add(invocation.getArgument(0));
            throw new UserProfileProvisioningException("Response lost after profile creation");
        }).when(userProfileClient).ensureProfileExists(any(UUID.class));

        try {
            assertThatThrownBy(() -> userAuthService.register(request))
                    .isInstanceOf(UserProfileProvisioningException.class);

            UserAuth pending = userAuthRepository.findByEmail(email).orElseThrow();
            assertThat(pending.isRegistrationCompleted()).isFalse();
            assertThat(provisionedProfiles).containsExactly(pending.getId());
            assertThatThrownBy(() -> loginService.login(new LoginRequest(email, password)))
                    .isInstanceOf(DisabledException.class);
            assertThatThrownBy(() -> userAuthService.register(
                    new RegisterRequest(email, "differentPassword12")))
                    .isInstanceOf(DuplicateEmailException.class);

            doAnswer(invocation -> {
                provisionedProfiles.add(invocation.getArgument(0));
                return null;
            }).when(userProfileClient).ensureProfileExists(any(UUID.class));

            userAuthService.register(request);

            UserAuth completed = userAuthRepository.findByEmail(email).orElseThrow();
            assertThat(completed.getId()).isEqualTo(pending.getId());
            assertThat(completed.getHashedPassword()).isEqualTo(pending.getHashedPassword());
            assertThat(completed.isRegistrationCompleted()).isTrue();
            assertThat(provisionedProfiles).containsExactly(pending.getId());
            assertThat(loginService.login(new LoginRequest(email, password)).accessToken()).isNotBlank();
            assertThatThrownBy(() -> userAuthService.register(request))
                    .isInstanceOf(DuplicateEmailException.class);
        } finally {
            userAuthRepository.findByEmail(email).ifPresent(userAuthRepository::delete);
        }
    }

    @Test
    void healthIsPublicButInfoStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
    }
}
