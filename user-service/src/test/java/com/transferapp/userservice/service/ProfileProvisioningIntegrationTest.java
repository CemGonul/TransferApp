package com.transferapp.userservice.service;

import com.transferapp.userservice.entity.UserProfile;
import com.transferapp.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileProvisioningIntegrationTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void concurrentProvisioningCreatesOneProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = executor.submit(() -> {
                start.await();
                return userProfileService.ensureProfileExists(userId);
            });
            Future<Boolean> second = executor.submit(() -> {
                start.await();
                return userProfileService.ensureProfileExists(userId);
            });
            start.countDown();

            boolean firstCreated = first.get(10, TimeUnit.SECONDS);
            boolean secondCreated = second.get(10, TimeUnit.SECONDS);
            assertThat(firstCreated ^ secondCreated).isTrue();
            assertThat(userProfileRepository.findById(userId)).isPresent();
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
            userProfileRepository.deleteById(userId);
        }
    }

    @Test
    void repeatedProvisioningDoesNotOverwritePersonalInformation() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile(userId);
        profile.updatePersonalInformation("Ali", "Yilmaz", "+48123456789", "PL");
        userProfileRepository.saveAndFlush(profile);
        try {
            assertThat(userProfileService.ensureProfileExists(userId)).isFalse();

            UserProfile existing = userProfileRepository.findById(userId).orElseThrow();
            assertThat(existing.getFirstName()).isEqualTo("Ali");
            assertThat(existing.getCountryCode()).isEqualTo("PL");
            assertThat(existing.getStatus()).isEqualTo(profile.getStatus());
        } finally {
            userProfileRepository.deleteById(userId);
        }
    }

    @Test
    void healthIsPublicButInfoStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
    }
}
