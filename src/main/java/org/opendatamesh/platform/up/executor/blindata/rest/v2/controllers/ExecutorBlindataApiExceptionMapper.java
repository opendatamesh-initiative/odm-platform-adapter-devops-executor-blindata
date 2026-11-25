package org.opendatamesh.platform.up.executor.blindata.rest.v2.controllers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.opendatamesh.platform.up.executor.blindata.exceptions.ExecutorBlindataApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The service's single error contract: the status of the exception and its message as plain text, which DevOps shows. */
@Provider
public class ExecutorBlindataApiExceptionMapper implements ExceptionMapper<ExecutorBlindataApiException> {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorBlindataApiExceptionMapper.class);

    @Override
    public Response toResponse(ExecutorBlindataApiException exception) {
        if (exception.getStatus().getFamily() == Response.Status.Family.SERVER_ERROR) {
            logger.warn("{}: {}", exception.getErrorName(), exception.getMessage());
        }
        return Response.status(exception.getStatus())
                .entity(exception.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
