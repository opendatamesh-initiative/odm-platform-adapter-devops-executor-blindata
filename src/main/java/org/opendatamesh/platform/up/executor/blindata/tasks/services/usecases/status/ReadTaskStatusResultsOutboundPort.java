package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;

interface ReadTaskStatusResultsOutboundPort {

    /** True when the task results can be posted to DevOps at all (service configured). */
    boolean canPublish();

    void publish(VerificationReport report);
}
