package com.feedback.platform.reporter.adapters.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Objects;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class SecurityInterceptor implements ContainerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityInterceptor.class);

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_CONTEXT_PROPERTY = "userContext";

    private final TokenValidator tokenValidator;
    private final String serviceName;
    private final boolean enabled;

    public SecurityInterceptor() {
        this(
                null,
                getConfigValue("security.interceptor.service-name", "feedback-reporter"),
                getBooleanConfigValue("security.interceptor.enabled", false)
        );
    }

    SecurityInterceptor(TokenValidator tokenValidator, String serviceName, boolean enabled) {
        this.tokenValidator = tokenValidator;
        this.serviceName = serviceName == null || serviceName.isBlank() ? "unknown-service" : serviceName;
        this.enabled = enabled;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!enabled) {
            return;
        }

        String authHeader = requestContext.getHeaderString(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            logFailure(401, "anonymous", "TOKEN_INVALID");
            throw TokenValidationException.invalidToken("Authorization Bearer token is required");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        String sub = extractSubject(token);

        try {
            TokenValidator validator = tokenValidator != null ? tokenValidator : new TokenValidator();
            UserContext userContext = validator.validate(token);
            requestContext.setProperty(USER_CONTEXT_PROPERTY, userContext);
            logSuccess(200, userContext.sub());
        } catch (TokenValidationException exception) {
            logFailure(exception.getStatusCode(), sub, exception.getErrorCode());
            throw exception;
        }
    }

    private String extractSubject(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims == null || claims.getSubject() == null || claims.getSubject().isBlank()) {
                return "anonymous";
            }
            return claims.getSubject();
        } catch (ParseException exception) {
            return "anonymous";
        }
    }

    private void logSuccess(int statusCode, String sub) {
        LOG.info(
                "{\"status_code\":{},\"sub\":\"{}\",\"service_name\":\"{}\"}",
                statusCode,
                sanitizeJson(sub),
                sanitizeJson(serviceName)
        );
    }

    private void logFailure(int statusCode, String sub, String errorCode) {
        LOG.warn(
                "{\"status_code\":{},\"sub\":\"{}\",\"service_name\":\"{}\",\"error_code\":\"{}\"}",
                statusCode,
                sanitizeJson(sub),
                sanitizeJson(serviceName),
                sanitizeJson(errorCode)
        );
    }

    private static String sanitizeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String getConfigValue(String key, String defaultValue) {
        Config config = ConfigProvider.getConfig();
        String value = config.getOptionalValue(key, String.class).orElse(defaultValue);
        return Objects.requireNonNullElse(value, defaultValue);
    }

    private static boolean getBooleanConfigValue(String key, boolean defaultValue) {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue(key, Boolean.class).orElse(defaultValue);
    }
}
