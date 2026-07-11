package com.feedback.platform.reporter.resource;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class NoAuthTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("security.interceptor.enabled", "false");
    }
}
