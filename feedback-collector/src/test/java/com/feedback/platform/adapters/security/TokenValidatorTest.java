package com.feedback.platform.adapters.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenValidatorTest {

    private static final String ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_TESTPOOL";
    private static final String AUDIENCE = "feedback-platform-api-dev";
    private static final String KEY_ID = "api-gateway-kid";

    @Mock
    private Clock clock;

    private RSAPrivateKey privateKey;
    private String jwkSetJson;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyID(KEY_ID)
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .build();

        jwkSetJson = new JWKSet(jwk).toString();
    }

    @Test
    void shouldReturnUserContextWhenTokenIsValid() {
        Instant now = Instant.parse("2026-07-04T20:00:00Z");
        when(clock.instant()).thenReturn(now);

        String token = createToken(
                now.plusSeconds(900),
                List.of("ADMIN", "SYSTEM"),
                "feedback:read feedback:write"
        );

        TokenValidator validator = new TokenValidator(ISSUER, AUDIENCE, jwkSetJson, clock);

        UserContext userContext = validator.validate(token);

        assertNotNull(userContext);
        assertEquals("user-123", userContext.sub());
        assertTrue(userContext.roles().contains("ADMIN"));
        assertTrue(userContext.roles().contains("SYSTEM"));
        assertTrue(userContext.roles().contains("feedback:read"));
        assertTrue(userContext.roles().contains("feedback:write"));
    }

    @Test
    void shouldThrowWhenTokenIsExpired() {
        Instant now = Instant.parse("2026-07-04T20:00:00Z");
        when(clock.instant()).thenReturn(now);

        String token = createToken(
                now.minusSeconds(60),
                List.of("ADMIN"),
                "feedback:read"
        );

        TokenValidator validator = new TokenValidator(ISSUER, AUDIENCE, jwkSetJson, clock);

        TokenValidationException exception = assertThrows(TokenValidationException.class, () -> validator.validate(token));

        assertEquals("TOKEN_EXPIRED", exception.getErrorCode());
        assertEquals(401, exception.getStatusCode());
        assertEquals("JWT is expired", exception.getMessage());
    }

    @Test
    void shouldThrowForbiddenWhenTokenHasNoScopeOrGroups() {
        Instant now = Instant.parse("2026-07-04T20:00:00Z");
        when(clock.instant()).thenReturn(now);

        String token = createToken(
                now.plusSeconds(900),
                null,
                null
        );

        TokenValidator validator = new TokenValidator(ISSUER, AUDIENCE, jwkSetJson, clock);

        TokenValidationException exception = assertThrows(TokenValidationException.class, () -> validator.validate(token));

        assertEquals("INSUFFICIENT_PERMISSION", exception.getErrorCode());
        assertEquals(403, exception.getStatusCode());
        assertEquals("JWT must contain at least one role in 'groups' or 'scope'", exception.getMessage());
    }

    private String createToken(Instant expiration, List<String> groups, String scope) {
        var builder = Jwts.builder()
                .setHeaderParam("kid", KEY_ID)
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setSubject("user-123")
                .setExpiration(Date.from(expiration))
                .setIssuedAt(Date.from(expiration.minusSeconds(300)));

        if (groups != null) {
            builder.claim("groups", groups);
        }

        if (scope != null) {
            builder.claim("scope", scope);
        }

        return builder
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}
