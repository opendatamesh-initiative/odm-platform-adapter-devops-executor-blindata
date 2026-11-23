package org.opendatamesh.platform.up.executor.blindata.exceptions;

import jakarta.ws.rs.core.Response;

public abstract class ExecutorBlindataApiException extends RuntimeException {

    private static final long serialVersionUID = 3876573329263306459L;

    public ExecutorBlindataApiException() {
        super();
    }

    public ExecutorBlindataApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ExecutorBlindataApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutorBlindataApiException(String message) {
        super(message);
    }

    public ExecutorBlindataApiException(Throwable cause) {
        super(cause);
    }

    public abstract String getErrorName();

    public abstract Response.Status getStatus();
}
