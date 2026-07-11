package com.feedback.platform.adapters.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TokenValidator {

    private static final Logger LOG = LoggerFactory.getLogger(TokenValidator.class);

    public static final String ENV_JWK_SET_JSON = "JWT_JWK_SET_JSON";
    public static final String ENV_EXPECTED_ISSUER = "JWT_EXPECTED_ISSUER";
    public static final String ENV_EXPECTED_AUDIENCE = "JWT_EXPECTED_AUDIENCE";

    private final String expectedIssuer;
    private final String expectedAudience;
    private final JWKSource<SecurityContext> jwkSource;
    private final DefaultJWSVerifierFactory verifierFactory;
    private final Clock clock;

    public TokenValidator() {
        this(
                requireEnv(ENV_EXPECTED_ISSUER),
                requireEnv(ENV_EXPECTED_AUDIENCE),
                requireEnv(ENV_JWK_SET_JSON),
                Clock.systemUTC()
        );
    }

    public TokenValidator(String expectedIssuer, String expectedAudience, String jwkSetJson, Clock clock) {
        this.expectedIssuer = requireNonBlank(expectedIssuer, "expectedIssuer");
        this.expectedAudience = requireNonBlank(expectedAudience, "expectedAudience");
        this.jwkSource = createJwkSource(requireNonBlank(jwkSetJson, "jwkSetJson"));
        this.verifierFactory = new DefaultJWSVerifierFactory();
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    public UserContext validate(String token) {
        try {
            SignedJWT signedJWT = parseToken(token);
            verifySignature(signedJWT);

            JWTClaimsSet claims = getClaims(signedJWT);
            validateIssuer(claims);
            validateAudience(claims);
            validateExpiration(claims);

            String subject = requireClaimNonBlank(claims.getSubject(), "sub");
            Set<String> roles = extractRoles(claims);

            logValidationSuccess(subject);
            return new UserContext(subject, roles);
        } catch (TokenValidationException exception) {
            logValidationFailure(exception.getErrorCode(), exception.getMessage(), exception.getReason());
            throw exception;
        }
    }

    private static JWKSource<SecurityContext> createJwkSource(String jwkSetJson) {
        try {
            JWKSet jwkSet = JWKSet.parse(jwkSetJson);
            return new ImmutableJWKSet<>(jwkSet);
        } catch (ParseException exception) {
            throw TokenValidationException.invalidToken("Invalid JWT JWK set JSON", exception);
        }
    }

    private SignedJWT parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw TokenValidationException.invalidToken("idToken/accessToken is required");
        }

        try {
            return SignedJWT.parse(token);
        } catch (ParseException exception) {
            throw TokenValidationException.invalidToken("Invalid JWT format", exception);
        }
    }

    private void verifySignature(SignedJWT signedJWT) {
        JWSHeader header = signedJWT.getHeader();
        JWSAlgorithm algorithm = header.getAlgorithm();

        if (algorithm == null || JWSAlgorithm.NONE.equals(algorithm)) {
            throw invalidToken("JWT without a valid signing algorithm is not accepted", "invalid_signature");
        }

        JWSVerificationKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(algorithm, jwkSource);
        List<Key> keys;
        try {
            keys = selector.selectJWSKeys(header, null);
        } catch (JOSEException exception) {
            throw invalidToken("Unable to resolve signature verification key", "invalid_signature", exception);
        }

        for (Key key : keys) {
            try {
                JWSVerifier verifier = verifierFactory.createJWSVerifier(header, key);
                if (verifier != null && signedJWT.verify(verifier)) {
                    return;
                }
            } catch (JOSEException exception) {
                // Try next candidate key when the current one is not applicable.
            }
        }

        throw invalidToken("JWT signature validation failed", "invalid_signature");
    }

    private JWTClaimsSet getClaims(SignedJWT signedJWT) {
        try {
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException exception) {
            throw TokenValidationException.invalidToken("Unable to parse JWT claims", exception);
        }
    }

    private void validateIssuer(JWTClaimsSet claims) {
        String issuer = requireClaimNonBlank(claims.getIssuer(), "iss");
        if (!expectedIssuer.equals(issuer)) {
            throw invalidToken("Invalid JWT issuer", "invalid_issuer");
        }
    }

    private void validateAudience(JWTClaimsSet claims) {
        List<String> audiences = claims.getAudience();
        if (audiences == null || audiences.stream().noneMatch(expectedAudience::equals)) {
            throw invalidToken("Invalid JWT audience", "invalid_audience");
        }
    }

    private void validateExpiration(JWTClaimsSet claims) {
        Date expirationTime = claims.getExpirationTime();
        if (expirationTime == null) {
            throw TokenValidationException.invalidToken("JWT claim 'exp' is required");
        }

        Instant now = clock.instant();
        if (!expirationTime.toInstant().isAfter(now)) {
            throw tokenExpired("JWT is expired", "expired");
        }
    }

    private Set<String> extractRoles(JWTClaimsSet claims) {
        Set<String> roles = new LinkedHashSet<>();

        Object groupsClaim = claims.getClaim("groups");
        if (groupsClaim instanceof Collection<?> groups) {
            groups.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .forEach(roles::add);
        } else if (groupsClaim instanceof String groupsString) {
            splitBySpaceOrComma(groupsString).forEach(roles::add);
        }

        String scope;
        try {
            scope = claims.getStringClaim("scope");
        } catch (ParseException exception) {
            throw TokenValidationException.invalidToken("Invalid JWT claim 'scope'", exception);
        }
        splitBySpaceOrComma(scope).forEach(roles::add);

        if (roles.isEmpty()) {
            throw forbidden("JWT must contain at least one role in 'groups' or 'scope'", "missing_scope_or_groups");
        }

        return Set.copyOf(roles);
    }

    private static Set<String> splitBySpaceOrComma(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Set.of();
        }

        Set<String> values = new LinkedHashSet<>();
        for (String token : rawValue.trim().split("[\\s,]+")) {
            if (!token.isBlank()) {
                values.add(token);
            }
        }
        return values;
    }

    private static String requireEnv(String key) {
        return requireNonBlank(System.getenv(key), "Missing environment variable: " + key);
    }

    private static String requireClaimNonBlank(String value, String claim) {
        if (value == null || value.isBlank()) {
            throw invalidToken("JWT claim '" + claim + "' is required", "missing_" + claim);
        }
        return value;
    }

    private static TokenValidationException invalidToken(String message, String reason) {
        return new TokenValidationException("TOKEN_INVALID", message, 401, reason);
    }

    private static TokenValidationException invalidToken(String message, String reason, Throwable cause) {
        return new TokenValidationException("TOKEN_INVALID", message, 401, reason, cause);
    }

    private static TokenValidationException tokenExpired(String message, String reason) {
        return new TokenValidationException("TOKEN_EXPIRED", message, 401, reason);
    }

    private static TokenValidationException forbidden(String message, String reason) {
        return new TokenValidationException("INSUFFICIENT_PERMISSION", message, 403, reason);
    }

    private static void logValidationSuccess(String subject) {
        LOG.info("{\"event\":\"TOKEN_VALIDATION_SUCCESS\",\"sub\":\"{}\"}", sanitizeJson(subject));
    }

    private static void logValidationFailure(String errorCode, String message, String reason) {
        LOG.warn(
                "{\"event\":\"TOKEN_VALIDATION_FAILURE\",\"error_code\":\"{}\",\"reason\":\"{}\",\"message\":\"{}\"}",
                sanitizeJson(errorCode),
                sanitizeJson(reason),
                sanitizeJson(message)
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

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}