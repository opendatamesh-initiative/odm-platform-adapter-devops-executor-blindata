package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import java.util.Optional;

/**
 * Identifier of a run returned to DevOps: {@code <environment>:<agent job sequence id>}. The environment travels in the
 * id because the status, logs and cancel calls do not receive the pipeline parameters.
 */
public record ProviderRunId(String environment, long sequenceId) {

    private static final char SEPARATOR = ':';

    public static Optional<ProviderRunId> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        int separator = value.lastIndexOf(SEPARATOR);
        if (separator <= 0 || separator == value.length() - 1) {
            return Optional.empty();
        }
        try {
            long sequenceId = Long.parseLong(value.substring(separator + 1));
            return sequenceId < 0 ? Optional.empty() : Optional.of(new ProviderRunId(value.substring(0, separator), sequenceId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String value() {
        return environment + SEPARATOR + sequenceId;
    }
}
