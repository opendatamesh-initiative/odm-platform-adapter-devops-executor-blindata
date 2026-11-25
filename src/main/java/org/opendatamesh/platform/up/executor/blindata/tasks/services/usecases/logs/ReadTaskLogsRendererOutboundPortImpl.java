package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;

class ReadTaskLogsRendererOutboundPortImpl implements ReadTaskLogsRendererOutboundPort {

    private final ReportTableRenderer renderer = new ReportTableRenderer();

    @Override
    public String render(VerificationReport report, String agentLog) {
        return renderer.render(report, agentLog);
    }
}
