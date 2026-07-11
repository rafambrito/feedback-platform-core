package com.feedback.platform.reporter.adapters.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record UserContext(String sub, Set<String> roles) {

    public UserContext {
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("JWT claim 'sub' is required");
        }
        Objects.requireNonNull(roles, "roles is required");

        Set<String> sanitizedRoles = roles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (sanitizedRoles.isEmpty()) {
            throw new IllegalArgumentException("At least one role is required");
        }

        roles = Collections.unmodifiableSet(sanitizedRoles);
    }
}
