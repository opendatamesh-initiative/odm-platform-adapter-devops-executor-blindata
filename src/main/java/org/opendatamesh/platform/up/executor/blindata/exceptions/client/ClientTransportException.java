package org.opendatamesh.platform.up.executor.blindata.exceptions.client;

import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;

/**
 * A call that got no HTTP response: connection refused, unknown host, connect timeout, or no answer in time. It keeps
 * code 500 so retry rules based on the code are unchanged; error messages can tell it apart from a real HTTP 500.
 */
public class ClientTransportException extends ClientException {

    private static final long serialVersionUID = 1L;

    public ClientTransportException(String message, Throwable cause) {
        super(500, message);
        initCause(cause);
    }

    /** True when the request was sent but no answer came back in time: the remote side may have acted on it. */
    public boolean isResponseTimeout() {
        return getCause() instanceof HttpTimeoutException && !(getCause() instanceof HttpConnectTimeoutException);
    }

    /** True when this service stopped waiting because its thread was interrupted (e.g. shutdown), not the network. */
    public boolean isInterrupted() {
        return getCause() instanceof InterruptedException;
    }

    @Override
    public String getMessage() {
        String detail = getResponseBody();
        if (detail == null || detail.isBlank()) {
            detail = getCause() == null ? "unknown cause" : getCause().getClass().getSimpleName();
        }
        return "no HTTP response: " + detail;
    }
}
