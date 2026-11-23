package org.opendatamesh.platform.up.executor.blindata.clients.devops;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;

/** ODM DevOps v2: the results of the task being run. */
public interface DevOpsClient {

    /** True when the DevOps service is configured as active, so results can be posted. */
    boolean isActive();

    /**
     * Appends the report as a JSON result of the task, addressed by data product, version tag, activity and task name.
     * DevOps accepts it only while the task is running.
     */
    void postResults(VerificationReport report);
}
