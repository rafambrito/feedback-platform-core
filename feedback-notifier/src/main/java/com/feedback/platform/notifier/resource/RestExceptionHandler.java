package com.feedback.platform.notifier.resource;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

@Provider
public class RestExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(RestExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {
        LOG.errorf(exception, "Erro não tratado: %s", exception.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", exception.getMessage());
        errorResponse.put("type", exception.getClass().getSimpleName());
        errorResponse.put("timestamp", System.currentTimeMillis());

        int statusCode = getStatusCode(exception);

        return Response.status(statusCode)
                .entity(errorResponse)
                .build();
    }

    private int getStatusCode(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }
        if (exception instanceof NullPointerException) {
            return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
}
