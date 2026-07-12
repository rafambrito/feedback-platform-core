package com.feedback.platform.reporter.adapters.observability;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION - 10)
public class RequestCorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    private static final String REQUEST_ID_KEY = "request_id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = resolveRequestId(requestContext);
        MDC.put(REQUEST_ID_KEY, requestId);
        requestContext.setProperty(REQUEST_ID_KEY, requestId);
        LOG.info(
                "{\"event\":\"REQUEST_RECEIVED\",\"request_id\":\"{}\",\"method\":\"{}\",\"path\":\"{}\"}",
                requestId,
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath()
        );
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Object requestId = requestContext.getProperty(REQUEST_ID_KEY);
        if (requestId instanceof String value && !value.isBlank()) {
            responseContext.getHeaders().putSingle(REQUEST_ID_HEADER, value);
            responseContext.getHeaders().putSingle(TRACE_ID_HEADER, value);
            LOG.info(
                    "{\"event\":\"REQUEST_COMPLETED\",\"request_id\":\"{}\",\"status_code\":{},\"method\":\"{}\",\"path\":\"{}\"}",
                    value,
                    responseContext.getStatus(),
                    requestContext.getMethod(),
                    requestContext.getUriInfo().getPath()
            );
        }
        MDC.remove(REQUEST_ID_KEY);
    }

    private String resolveRequestId(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = requestContext.getHeaderString(TRACE_ID_HEADER);
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }
}
