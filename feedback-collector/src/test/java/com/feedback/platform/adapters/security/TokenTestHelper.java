package com.feedback.platform.adapters.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Utilitário para gerar tokens JWT de teste.
 * Usa RSA 2048 e JJWT/Nimbus para criação e serialização.
 */
public class TokenTestHelper {

    private static final String ISSUER = "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_TESTPOOL";
    private static final String AUDIENCE = "feedback-platform-api-dev";
    private static final String KEY_ID = "api-gateway-kid";
    private static final String SUBJECT = "test-user-12345";

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String jwkSetJson;

    public TokenTestHelper() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();

        RSAKey jwk = new RSAKey.Builder(publicKey)
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.RS256)
                .build();

        this.jwkSetJson = new JWKSet(jwk).toString();
    }

    /**
     * Gera um token JWT válido com escopos.
     */
    public String generateValidToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("kid", KEY_ID)
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setSubject(SUBJECT)
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .setIssuedAt(Date.from(now))
                .claim("groups", List.of("ADMIN", "SYSTEM"))
                .claim("scope", "feedback:read feedback:write")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Gera um token JWT expirado.
     */
    public String generateExpiredToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("kid", KEY_ID)
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setSubject(SUBJECT)
                .setExpiration(Date.from(now.minusSeconds(600)))
                .setIssuedAt(Date.from(now.minusSeconds(1200)))
                .claim("groups", List.of("ADMIN"))
                .claim("scope", "feedback:read")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Gera um token JWT sem escopo ou groups (deve resultar em 403).
     */
    public String generateTokenWithoutScopes() {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("kid", KEY_ID)
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setSubject(SUBJECT)
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .setIssuedAt(Date.from(now))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String getJwkSetJson() {
        return jwkSetJson;
    }

    public String getIssuer() {
        return ISSUER;
    }

    public String getAudience() {
        return AUDIENCE;
    }

    /**
     * Main method para gerar tokens e exibir os valores prontos para uso em testes.
     */
    public static void main(String[] args) throws Exception {
        TokenTestHelper helper = new TokenTestHelper();

        String validToken = helper.generateValidToken();
        String expiredToken = helper.generateExpiredToken();
        String noScopesToken = helper.generateTokenWithoutScopes();
        String jwkSet = helper.getJwkSetJson();
        String issuer = helper.getIssuer();
        String audience = helper.getAudience();

        System.out.println("=== TOKEN TEST HELPER OUTPUT ===\n");

        System.out.println("# Configuração para application.properties:");
        System.out.println("JWT_EXPECTED_ISSUER=" + issuer);
        System.out.println("JWT_EXPECTED_AUDIENCE=" + audience);
        System.out.println("JWT_JWK_SET_JSON=" + jwkSet.replace("\"", "\\\""));
        System.out.println("security.interceptor.enabled=true");
        System.out.println("security.interceptor.service-name=feedback-collector\n");

        System.out.println("# Token válido (com escopos):");
        System.out.println(validToken);
        System.out.println("\n# Token expirado:");
        System.out.println(expiredToken);
        System.out.println("\n# Token sem escopos (deve resultar em 403):");
        System.out.println(noScopesToken);

        System.out.println("\n=== CURL COMMANDS FOR TESTING ===\n");

        System.out.println("# Teste 1: Sem token (esperado 401)");
        System.out.println("curl -i -X POST http://localhost:8080/feedback \\");
        System.out.println("  -H 'Content-Type: application/json' \\");
        System.out.println("  -d '{\"cursoId\":\"C1\",\"alunoId\":\"A1\",\"professorId\":\"P1\",\"nota\":10,\"comentario\":\"sem token\"}'\n");

        System.out.println("# Teste 2: Token inválido (esperado 401)");
        System.out.println("curl -i -X POST http://localhost:8080/feedback \\");
        System.out.println("  -H 'Authorization: Bearer INVALID_TOKEN_HERE' \\");
        System.out.println("  -H 'Content-Type: application/json' \\");
        System.out.println("  -d '{\"cursoId\":\"C1\",\"alunoId\":\"A1\",\"professorId\":\"P1\",\"nota\":10,\"comentario\":\"token invalido\"}'\n");

        System.out.println("# Teste 3: Token expirado (esperado 401)");
        System.out.println("curl -i -X POST http://localhost:8080/feedback \\");
        System.out.println("  -H 'Authorization: Bearer " + expiredToken + "' \\");
        System.out.println("  -H 'Content-Type: application/json' \\");
        System.out.println("  -d '{\"cursoId\":\"C1\",\"alunoId\":\"A1\",\"professorId\":\"P1\",\"nota\":10,\"comentario\":\"token expirado\"}'\n");

        System.out.println("# Teste 4: Token sem escopos (esperado 403)");
        System.out.println("curl -i -X POST http://localhost:8080/feedback \\");
        System.out.println("  -H 'Authorization: Bearer " + noScopesToken + "' \\");
        System.out.println("  -H 'Content-Type: application/json' \\");
        System.out.println("  -d '{\"cursoId\":\"C1\",\"alunoId\":\"A1\",\"professorId\":\"P1\",\"nota\":10,\"comentario\":\"sem escopos\"}'\n");

        System.out.println("# Teste 5: Token válido (esperado 201)");
        System.out.println("curl -i -X POST http://localhost:8080/feedback \\");
        System.out.println("  -H 'Authorization: Bearer " + validToken + "' \\");
        System.out.println("  -H 'Content-Type: application/json' \\");
        System.out.println("  -d '{\"cursoId\":\"C1\",\"alunoId\":\"A1\",\"professorId\":\"P1\",\"nota\":10,\"comentario\":\"token valido\"}'\n");
    }
}
