package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;

interface ReadTaskLogsRendererOutboundPort {

    /** The report as readable text, followed by the agent job log. */
    String render(VerificationReport report, String agentLog);
}
