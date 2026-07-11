package com.feedback.platform.resource;

import com.feedback.platform.adapters.security.TokenTestHelper;
import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class SecurityEnabledTestProfile implements QuarkusTestProfile {

    static final TokenTestHelper TOKENS;

    static {
        try {
            TOKENS = new TokenTestHelper();
            System.setProperty("JWT_EXPECTED_ISSUER", TOKENS.getIssuer());
            System.setProperty("JWT_EXPECTED_AUDIENCE", TOKENS.getAudience());
            System.setProperty("JWT_JWK_SET_JSON", TOKENS.getJwkSetJson());
        } catch (Exception exception) {
            throw new RuntimeException("Falha ao inicializar TokenTestHelper", exception);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "security.interceptor.enabled", "true",
                "security.interceptor.service-name", "feedback-collector",
                "JWT_EXPECTED_ISSUER", TOKENS.getIssuer(),
                "JWT_EXPECTED_AUDIENCE", TOKENS.getAudience(),
                "JWT_JWK_SET_JSON", TOKENS.getJwkSetJson(),
                "aws.eventbridge.endpoint.override", "http://localhost:4010"
        );
    }
}
