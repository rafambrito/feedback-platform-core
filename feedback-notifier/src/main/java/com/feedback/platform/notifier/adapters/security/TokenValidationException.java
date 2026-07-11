package com.feedback.platform.notifier.adapters.security;

public class TokenValidationException extends RuntimeException {

    private final String errorCode;
    private final int statusCode;
    private final String reason;

    public TokenValidationException(String errorCode, String message, int statusCode) {
        this(errorCode, message, statusCode, "unknown", null);
    }

    public TokenValidationException(String errorCode, String message, int statusCode, Throwable cause) {
        this(errorCode, message, statusCode, "unknown", cause);
    }

    public TokenValidationException(String errorCode, String message, int statusCode, String reason) {
        this(errorCode, message, statusCode, reason, null);
    }

    public TokenValidationException(String errorCode, String message, int statusCode, String reason, Throwable cause) {
        super(message);
        if (cause != null) {
            initCause(cause);
        }
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.reason = reason == null || reason.isBlank() ? "unknown" : reason;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getReason() {
        return reason;
    }

    public static TokenValidationException invalidToken(String message) {
        return new TokenValidationException("TOKEN_INVALID", message, 401);
    }

    public static TokenValidationException invalidToken(String message, Throwable cause) {
        return new TokenValidationException("TOKEN_INVALID", message, 401, cause);
    }

    public static TokenValidationException tokenExpired(String message) {
        return new TokenValidationException("TOKEN_EXPIRED", message, 401);
    }

    public static TokenValidationException forbidden(String message) {
        return new TokenValidationException("INSUFFICIENT_PERMISSION", message, 403);
    }
}
