package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

/**
 * Start of a contract verification.
 *
 * @param versionTag         the release tag DevOps sends as {@code executorParameters.ref.name}
 * @param dataProductVersion optional version number; when present it wins over the tag
 * @param activityName       optional; with {@code taskName} it lets the report be posted to DevOps
 */
public record StartTaskCommand(String dataProductFqn, String environment, String versionTag, String dataProductVersion,
                               String activityName, String taskName) {
}
