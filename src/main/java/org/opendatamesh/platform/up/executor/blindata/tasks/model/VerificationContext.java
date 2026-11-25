package org.opendatamesh.platform.up.executor.blindata.tasks.model;

/**
 * What is being verified and where the report goes. {@code activityName} and {@code taskName} are optional: without
 * them the report is not posted to DevOps and stays in the task logs.
 */
public record VerificationContext(String dataProductFqn, String dataProductVersion, String versionTag,
                                  String environment, String activityName, String taskName) {
}
