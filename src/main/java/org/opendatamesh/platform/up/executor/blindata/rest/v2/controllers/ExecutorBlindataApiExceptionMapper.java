package org.opendatamesh.platform.up.executor.blindata.rest.v2.controllers;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.opendatamesh.platform.up.executor.blindata.exceptions.ExecutorBlindataApiException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The service's single error contract: the status of the exception and its message as plain text, which DevOps
 * records in the task logs. Every error answered is logged with the endpoint that answered it: an unexpected failure
 * at ERROR with its stack trace, a refused request or a failing remote service at WARN with its cause.
 */
@Provider
public class ExecutorBlindataApiExceptionMapper implements ExceptionMapper<ExecutorBlindataApiException> {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorBlindataApiExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ExecutorBlindataApiException exception) {
        log(exception);
        return Response.status(exception.getStatus())
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }

    private void log(ExecutorBlindataApiException exception) {
        String endpoint = endpoint();
        int status = exception.getStatus().getStatusCode();
        if (exception instanceof InternalException) {
            logger.error("{} answered HTTP {} {}: {}", endpoint, status, exception.getErrorName(),
                    exception.getMessage(), exception);
            return;
        }
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().equals(exception.getMessage())) {
            logger.warn("{} answered HTTP {} {}: {} (cause: {}: {})", endpoint, status, exception.getErrorName(),
                    exception.getMessage(), cause.getClass().getSimpleName(), cause.getMessage());
        } else {
            logger.warn("{} answered HTTP {} {}: {}", endpoint, status, exception.getErrorName(), exception.getMessage());
        }
    }

    private String endpoint() {
        try {
            return uriInfo == null ? "Request" : "/" + uriInfo.getPath().replaceFirst("^/", "");
        } catch (RuntimeException outsideRequest) {
            return "Request";
        }
    }
}
