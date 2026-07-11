package com.feedback.platform.reporter.resource;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class SecurityEnabledTestProfile implements QuarkusTestProfile {

    static final SecurityTokenTestHelper TOKENS = new SecurityTokenTestHelper();

    static {
        System.setProperty("JWT_EXPECTED_ISSUER", TOKENS.getIssuer());
        System.setProperty("JWT_EXPECTED_AUDIENCE", TOKENS.getAudience());
        System.setProperty("JWT_JWK_SET_JSON", TOKENS.getJwkSetJson());
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "security.interceptor.enabled", "true",
                "security.interceptor.service-name", "feedback-reporter",
                "JWT_EXPECTED_ISSUER", TOKENS.getIssuer(),
                "JWT_EXPECTED_AUDIENCE", TOKENS.getAudience(),
                "JWT_JWK_SET_JSON", TOKENS.getJwkSetJson()
        );
    }
}
