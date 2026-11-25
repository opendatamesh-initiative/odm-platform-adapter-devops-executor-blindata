package org.opendatamesh.platform.up.executor.blindata.tasks.model;

/** Run status of a job in the Blindata agent history. {@link #UNKNOWN} stands for any value the executor does not know. */
public enum AgentRunStatus {
    RUNNING,
    SUCCESS,
    WARNING,
    ERROR,
    STOPPED,
    UNKNOWN;

    public static AgentRunStatus parse(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        for (AgentRunStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    public boolean hasEnded() {
        return this == SUCCESS || this == WARNING || this == ERROR || this == STOPPED;
    }
}
