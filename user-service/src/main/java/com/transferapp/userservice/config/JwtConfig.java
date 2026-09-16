package com.transferapp.userservice.config;

import com.transferapp.userservice.security.JwtAudienceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class JwtConfig {

    private final JwtProperties jwtProperties;

    private RSAPublicKey readPublicKey() throws Exception{

        String key=new String(
                readAllBytes(jwtProperties.publicKey()),
                StandardCharsets.UTF_8);

        key = key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded= Base64.getDecoder().decode(key);

        X509EncodedKeySpec keySpec=new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory=KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }
    @Bean
    public JwtDecoder jwtDecoder() throws Exception {

        RSAPublicKey publicKey = readPublicKey();

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(jwtProperties.issuer());

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtAudienceValidator(jwtProperties.audience());

        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                );

        decoder.setJwtValidator(validator);

        return decoder;
    }


    private byte[] readAllBytes(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }
}
