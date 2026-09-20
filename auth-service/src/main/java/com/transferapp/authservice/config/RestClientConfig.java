package com.transferapp.authservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(UserServiceProperties.class)
public class RestClientConfig {

    private final UserServiceProperties userServiceProperties;

    @Bean
    public RestClient userServiceRestClient() {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(userServiceProperties.connectTimeout());

        requestFactory.setReadTimeout(userServiceProperties.readTimeout());

        return RestClient.builder()
                .baseUrl(userServiceProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
