package com.feedback.platform.resource;

import com.feedback.platform.adapters.security.TokenValidationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.UUID;

@Provider
public class TokenValidationExceptionHandler implements ExceptionMapper<TokenValidationException> {

    @Context
    HttpHeaders httpHeaders;

    @Override
    public Response toResponse(TokenValidationException exception) {
        String traceId = resolveTraceId();
        Map<String, String> payload = Map.of(
                "error_code", exception.getErrorCode(),
                "message", exception.getMessage(),
                "trace_id", traceId
        );

        return Response.status(exception.getStatusCode())
                .type(MediaType.APPLICATION_JSON)
                .header("X-Trace-Id", traceId)
                .entity(payload)
                .build();
    }

    private String resolveTraceId() {
        if (httpHeaders == null) {
            return UUID.randomUUID().toString();
        }

        String traceId = httpHeaders.getHeaderString("X-Request-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = httpHeaders.getHeaderString("x-request-id");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = httpHeaders.getHeaderString("X-Trace-Id");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = httpHeaders.getHeaderString("x-trace-id");
        }

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        return traceId;
    }
}
