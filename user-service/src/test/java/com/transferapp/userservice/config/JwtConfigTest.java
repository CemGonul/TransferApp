package com.transferapp.userservice.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {

    private static final String ISSUER = "https://transferapp.local/auth-service";
    private static final String AUDIENCE = "transferapp-api";

    private KeyPair keyPair;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = generateKeyPair();
        JwtProperties properties = new JwtProperties(
                publicKeyResource((RSAPublicKey) keyPair.getPublic()),
                ISSUER,
                AUDIENCE
        );
        jwtDecoder = new JwtConfig(properties).jwtDecoder();
    }

    @Test
    void decodesValidToken() {
        UUID userId = UUID.randomUUID();
        String token = encodeToken(
                keyPair,
                ISSUER,
                List.of(AUDIENCE),
                Instant.now().plusSeconds(300),
                userId.toString()
        );

        var jwt = jwtDecoder.decode(token);

        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals("USER", jwt.getClaimAsString("role"));
        assertEquals(List.of(AUDIENCE), jwt.getAudience());
    }

    @Test
    void rejectsExpiredToken() {
        String token = encodeToken(
                keyPair,
                ISSUER,
                List.of(AUDIENCE),
                Instant.now().minusSeconds(120),
                UUID.randomUUID().toString()
        );

        assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }

    @Test
    void rejectsTokenWithWrongIssuer() {
        String token = encodeToken(
                keyPair,
                "https://wrong-issuer.example",
                List.of(AUDIENCE),
                Instant.now().plusSeconds(300),
                UUID.randomUUID().toString()
        );

        assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }

    @Test
    void rejectsTokenWithWrongAudience() {
        String token = encodeToken(
                keyPair,
                ISSUER,
                List.of("another-api"),
                Instant.now().plusSeconds(300),
                UUID.randomUUID().toString()
        );

        assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }

    @Test
    void rejectsTokenSignedWithDifferentPrivateKey() throws Exception {
        KeyPair differentKeyPair = generateKeyPair();
        String token = encodeToken(
                differentKeyPair,
                ISSUER,
                List.of(AUDIENCE),
                Instant.now().plusSeconds(300),
                UUID.randomUUID().toString()
        );

        assertThrows(JwtException.class, () -> jwtDecoder.decode(token));
    }

    private String encodeToken(
            KeyPair signingKeyPair,
            String issuer,
            List<String> audience,
            Instant expiresAt,
            String subject) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now)
                ? expiresAt.minusSeconds(300)
                : now;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .audience(audience)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", "USER")
                .build();

        return encoder(signingKeyPair)
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    private JwtEncoder encoder(KeyPair signingKeyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) signingKeyPair.getPublic())
                .privateKey((RSAPrivateKey) signingKeyPair.getPrivate())
                .build();
        JWKSource<SecurityContext> source =
                new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(source);
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private ByteArrayResource publicKeyResource(RSAPublicKey publicKey) {
        String encodedKey = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(publicKey.getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + encodedKey
                + "\n-----END PUBLIC KEY-----\n";
        return new ByteArrayResource(pem.getBytes(StandardCharsets.UTF_8));
    }
}
