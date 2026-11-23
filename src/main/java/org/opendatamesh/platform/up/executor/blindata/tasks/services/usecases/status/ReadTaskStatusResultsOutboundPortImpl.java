package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.blindata.clients.devops.DevOpsClient;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;

class ReadTaskStatusResultsOutboundPortImpl implements ReadTaskStatusResultsOutboundPort {

    private final DevOpsClient devOps;

    ReadTaskStatusResultsOutboundPortImpl(DevOpsClient devOps) {
        this.devOps = devOps;
    }

    @Override
    public boolean canPublish() {
        return devOps.isActive();
    }

    @Override
    public void publish(VerificationReport report) {
        devOps.postResults(report);
    }
}
