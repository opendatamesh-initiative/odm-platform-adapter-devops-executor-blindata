package org.opendatamesh.platform.up.executor.blindata.tasks.model;

public record ReportSummary(int total, int passed, int failed, int error, int skipped, int failedBlocking) {
}
