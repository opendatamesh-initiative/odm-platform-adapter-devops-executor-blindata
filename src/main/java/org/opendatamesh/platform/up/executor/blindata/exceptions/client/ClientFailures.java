package org.opendatamesh.platform.up.executor.blindata.exceptions.client;

/** Words a failed HTTP call for an error message: the HTTP status and body excerpt, or why there was no answer. */
public final class ClientFailures {

    private ClientFailures() {
    }

    /** {@code ": HTTP 404: <body>"}, {@code ": no connection (<error>)"} or {@code ": no answer in time (<error>)"}. */
    public static String describe(ClientException e, int maxBodyChars) {
        if (e instanceof ClientTransportException transport) {
            String kind = transport.isResponseTimeout() ? "no answer in time" : "no connection";
            String detail = excerpt(transport.getResponseBody(), maxBodyChars);
            return ": " + kind + (detail.isEmpty() ? "" : " (" + detail + ")");
        }
        String body = excerpt(e.getResponseBody(), maxBodyChars);
        return ": HTTP " + e.getCode() + (body.isEmpty() ? "" : ": " + body);
    }

    private static String excerpt(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.strip();
        return trimmed.length() > maxChars ? trimmed.substring(0, maxChars) + "…" : trimmed;
    }
}
