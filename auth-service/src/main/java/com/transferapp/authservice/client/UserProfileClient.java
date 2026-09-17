package com.transferapp.authservice.client;

import com.transferapp.authservice.exception.UserProfileProvisioningException;
import com.transferapp.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserProfileClient {

    private final RestClient userServiceRestClient;
    private final JwtService jwtService;

    public void ensureProfileExists(UUID userId) {

        String serviceToken = jwtService.generateServiceToken();

        try {
            ResponseEntity<Void> response = userServiceRestClient
                    .put()
                    .uri("/internal/user-profiles/{userId}", userId)
                    .headers(headers -> headers.setBearerAuth(serviceToken))
                    .retrieve()
                    .toBodilessEntity();

            HttpStatusCode status = response.getStatusCode();
            if (status.value() != HttpStatus.CREATED.value()
                    && status.value() != HttpStatus.NO_CONTENT.value()) {
                log.error(
                        "User profile provisioning failed for userId={} due to unexpected downstream status={}",
                        userId,
                        status
                );
                throw new UserProfileProvisioningException(
                        "Unexpected response from user-service"
                );
            }
        } catch (HttpClientErrorException exception) {
            log.error(
                    "User profile provisioning failed for userId={} due to downstream 4xx status={}",
                    userId,
                    exception.getStatusCode()
            );
            throw new UserProfileProvisioningException(
                    "User-service rejected profile provisioning",
                    exception
            );
        } catch (HttpServerErrorException exception) {
            log.error(
                    "User profile provisioning failed for userId={} due to downstream 5xx status={}",
                    userId,
                    exception.getStatusCode()
            );
            throw new UserProfileProvisioningException(
                    "User-service failed to provision the profile",
                    exception
            );
        } catch (ResourceAccessException exception) {
            log.error(
                    "User profile provisioning failed for userId={} due to timeout or network failure",
                    userId,
                    exception
            );
            throw new UserProfileProvisioningException(
                    "User-service is unreachable",
                    exception
            );
        } catch (RestClientException exception) {
            log.error(
                    "User profile provisioning failed for userId={} due to an unexpected HTTP client failure",
                    userId,
                    exception
            );
            throw new UserProfileProvisioningException(
                    "Unexpected user-service communication failure",
                    exception
            );
        }
    }
}
