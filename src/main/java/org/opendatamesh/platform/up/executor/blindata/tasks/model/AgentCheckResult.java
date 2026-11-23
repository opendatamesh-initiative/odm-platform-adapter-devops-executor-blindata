package org.opendatamesh.platform.up.executor.blindata.tasks.model;

/** One row of the agent job results ({@code results} or {@code errors}). */
public record AgentCheckResult(String checkCode, Double metric, Double totalElements, Integer score, String error) {

    public boolean isError() {
        return error != null && !error.isBlank();
    }
}
