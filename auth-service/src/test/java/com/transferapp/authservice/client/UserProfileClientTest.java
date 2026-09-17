package com.transferapp.authservice.client;

import com.transferapp.authservice.exception.UserProfileProvisioningException;
import com.transferapp.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class UserProfileClientTest {

    private MockRestServiceServer server;
    private UserProfileClient userProfileClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("http://user-service");
        server = MockRestServiceServer.bindTo(builder).build();

        JwtService jwtService = mock(JwtService.class);
        when(jwtService.generateServiceToken()).thenReturn("service-token");
        userProfileClient = new UserProfileClient(builder.build(), jwtService);
    }

    @Test
    void acceptsCreatedResponse() {
        UUID userId = UUID.randomUUID();
        expectRequest(userId, HttpStatus.CREATED);

        assertDoesNotThrow(() -> userProfileClient.ensureProfileExists(userId));

        server.verify();
    }

    @Test
    void acceptsNoContentResponse() {
        UUID userId = UUID.randomUUID();
        expectRequest(userId, HttpStatus.NO_CONTENT);

        assertDoesNotThrow(() -> userProfileClient.ensureProfileExists(userId));

        server.verify();
    }

    @Test
    void mapsDownstreamClientErrorToProvisioningException() {
        UUID userId = UUID.randomUUID();
        expectRequest(userId, HttpStatus.FORBIDDEN);

        assertThrows(
                UserProfileProvisioningException.class,
                () -> userProfileClient.ensureProfileExists(userId)
        );

        server.verify();
    }

    @Test
    void mapsDownstreamServerErrorToProvisioningException() {
        UUID userId = UUID.randomUUID();
        expectRequest(userId, HttpStatus.INTERNAL_SERVER_ERROR);

        assertThrows(
                UserProfileProvisioningException.class,
                () -> userProfileClient.ensureProfileExists(userId)
        );

        server.verify();
    }

    @Test
    void mapsNetworkFailureToProvisioningException() {
        UUID userId = UUID.randomUUID();
        server.expect(once(), requestTo(
                        "http://user-service/internal/user-profiles/" + userId))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withException(new IOException("connection refused")));

        assertThrows(
                UserProfileProvisioningException.class,
                () -> userProfileClient.ensureProfileExists(userId)
        );

        server.verify();
    }

    private void expectRequest(UUID userId, HttpStatus responseStatus) {
        server.expect(once(), requestTo(
                        "http://user-service/internal/user-profiles/" + userId))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token"))
                .andRespond(withStatus(responseStatus));
    }
}
