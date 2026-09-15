package com.transferapp.authservice.security;

import com.transferapp.authservice.config.JwtConfig;
import com.transferapp.authservice.config.JwtProperties;
import com.transferapp.authservice.entity.UserAuth;
import com.transferapp.authservice.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtProperties properties;
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        properties = new JwtProperties(
                pemResource("PRIVATE KEY", ((RSAPrivateKey) keyPair.getPrivate()).getEncoded()),
                pemResource("PUBLIC KEY", ((RSAPublicKey) keyPair.getPublic()).getEncoded()),
                "https://miniwise.local/auth-service",
                "miniwise-api",
                Duration.ofMinutes(15)
        );

        JwtConfig config = new JwtConfig(properties);
        encoder = config.jwtEncoder();
        decoder = config.jwtDecoder();
    }

    @Test
    void generatedTokenCanBeVerifiedWithExpectedClaims() {
        UserAuth user = mock(UserAuth.class);
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getRole()).thenReturn(Role.USER);

        JwtService jwtService = new JwtService(encoder, properties);
        var decoded = decoder.decode(jwtService.generateToken(user));

        assertAll(
                () -> assertEquals("RS256", decoded.getHeaders().get("alg")),
                () -> assertEquals("https://miniwise.local/auth-service", decoded.getIssuer().toString()),
                () -> assertEquals(userId.toString(), decoded.getSubject()),
                () -> assertEquals(List.of("miniwise-api"), decoded.getAudience()),
                () -> assertEquals("user@example.com", decoded.getClaimAsString("email")),
                () -> assertEquals("USER", decoded.getClaimAsString("role")),
                () -> assertEquals(900L, Duration.between(
                        decoded.getIssuedAt(), decoded.getExpiresAt()).toSeconds()),
                () -> assertEquals(900L, jwtService.getExpirationSeconds())
        );
    }

    @Test
    void decoderRejectsTokenForDifferentAudience() {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(UUID.randomUUID().toString())
                .audience(List.of("different-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    private ByteArrayResource pemResource(String type, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(encoded);
        String pem = "-----BEGIN " + type + "-----\n"
                + base64
                + "\n-----END " + type + "-----\n";
        return new ByteArrayResource(pem.getBytes(StandardCharsets.UTF_8));
    }
}
