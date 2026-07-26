package com.feedback.platform.reporter.adapters.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenValidatorTest {

    private static final String ISSUER = "https://issuer.example";
    private static final String AUDIENCE = "feedback-platform-api";
    private static final String KID = "test-key-id";

    private static RSAPrivateKey privateKey;
    private static String jwkSetJson;

    @BeforeAll
    static void setUpKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyID(KID)
                .algorithm(JWSAlgorithm.RS256)
                .build();

        jwkSetJson = new JWKSet(jwk).toString();
    }

    @Test
    void deveValidarTokenComSucesso() throws ParseException, JOSEException {
        Instant now = Instant.parse("2026-07-26T19:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        TokenValidator validator = new TokenValidator(ISSUER, AUDIENCE, jwkSetJson, fixedClock);

        String token = criarTokenAssinado(ISSUER, AUDIENCE, "user-123", "report.read", now.plusSeconds(300));

        UserContext userContext = validator.validate(token);

        assertEquals("user-123", userContext.sub());
        assertTrue(userContext.roles().contains("report.read"));
    }

    @Test
    void deveRejeitarTokenComAudienceInvalida() throws ParseException, JOSEException {
        Instant now = Instant.parse("2026-07-26T19:00:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        TokenValidator validator = new TokenValidator(ISSUER, AUDIENCE, jwkSetJson, fixedClock);

        String token = criarTokenAssinado(ISSUER, "outro-audience", "user-123", "report.read", now.plusSeconds(300));

        TokenValidationException exception = assertThrows(TokenValidationException.class, () -> validator.validate(token));
        assertEquals("TOKEN_INVALID", exception.getErrorCode());
        assertEquals(401, exception.getStatusCode());
        assertEquals("invalid_audience", exception.getReason());
    }

    private static String criarTokenAssinado(String issuer,
                                             String audience,
                                             String subject,
                                             String scope,
                                             Instant expiration) throws ParseException, JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(subject)
                .expirationTime(Date.from(expiration))
                .claim("scope", scope)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KID).build(),
                claimsSet
        );

        signedJWT.sign(new RSASSASigner(privateKey));
        return signedJWT.serialize();
    }
}
