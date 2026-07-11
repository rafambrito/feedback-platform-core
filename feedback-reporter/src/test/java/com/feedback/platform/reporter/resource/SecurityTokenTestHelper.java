package com.feedback.platform.reporter.resource;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class SecurityTokenTestHelper {

    private static final String ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_TESTPOOL";
    private static final String AUDIENCE = "feedback-platform-api-dev";
    private static final String KEY_ID = "api-gateway-kid";

    private final RSAPrivateKey privateKey;
    private final String jwkSetJson;

    public SecurityTokenTestHelper() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            privateKey = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

            RSAKey jwk = new RSAKey.Builder(publicKey)
                    .keyID(KEY_ID)
                    .algorithm(JWSAlgorithm.RS256)
                    .build();
            jwkSetJson = new JWKSet(jwk).toString();
        } catch (Exception exception) {
            throw new RuntimeException("Falha ao inicializar helper JWT", exception);
        }
    }

    public String generateValidToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("kid", KEY_ID)
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setSubject("user-reporter")
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .setIssuedAt(Date.from(now))
                .claim("groups", List.of("PROFESSOR"))
                .claim("scope", "report:read")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String getIssuer() {
        return ISSUER;
    }

    public String getAudience() {
        return AUDIENCE;
    }

    public String getJwkSetJson() {
        return jwkSetJson;
    }
}
