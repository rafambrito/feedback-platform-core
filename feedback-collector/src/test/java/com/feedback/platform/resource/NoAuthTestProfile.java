package com.feedback.platform.resource;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class NoAuthTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "security.interceptor.enabled", "false",
                "aws.eventbridge.endpoint.override", "http://localhost:4010"
        );
    }
}
