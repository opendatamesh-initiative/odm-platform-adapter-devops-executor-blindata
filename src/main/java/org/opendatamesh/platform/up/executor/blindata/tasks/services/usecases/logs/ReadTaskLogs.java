package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractVerdict;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCase;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCaseException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Builds the task logs: the full verification report, then the agent job log. The report is always written, so the
 * logs are the record of the run even when the results post to DevOps is off or fails; an unreadable agent log only
 * adds a line.
 */
class ReadTaskLogs implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReadTaskLogs.class);

    private final ReadTaskLogsCommand command;
    private final ReadTaskLogsPresenter presenter;
    private final ReadTaskLogsEnvironmentOutboundPort environmentPort;
    private final ReadTaskLogsAgentOutboundPort agentPort;
    private final ReadTaskLogsRendererOutboundPort rendererPort;

    ReadTaskLogs(ReadTaskLogsCommand command, ReadTaskLogsPresenter presenter,
                 ReadTaskLogsEnvironmentOutboundPort environmentPort, ReadTaskLogsAgentOutboundPort agentPort,
                 ReadTaskLogsRendererOutboundPort rendererPort) {
        this.command = command;
        this.presenter = presenter;
        this.environmentPort = environmentPort;
        this.agentPort = agentPort;
        this.rendererPort = rendererPort;
    }

    @Override
    public void execute() {
        try {
            ProviderRunId runId = parseRunId();
            EnvironmentProfile profile = findProfile(runId);
            AgentRun run = readRun(profile, runId);
            String content = rendererPort.render(ContractVerdict.evaluate(run), agentLog(profile, runId));
            logger.debug("Logs of run {} read: {} characters", runId.value(), content.length());
            presenter.presentLogs(content, Instant.now());
        } catch (Exception e) {
            presenter.presentException(e);
        }
    }

    private ProviderRunId parseRunId() {
        return ProviderRunId.parse(command.providerRunId())
                .orElseThrow(() -> new UseCaseException(Reason.INVALID,
                        "providerRunId must be <environment>:<agent job sequence id>, got " + command.providerRunId()));
    }

    private EnvironmentProfile findProfile(ProviderRunId runId) {
        return environmentPort.findProfile(runId.environment())
                .orElseThrow(() -> new UseCaseException(Reason.INVALID,
                        "Unknown environment " + runId.environment() + " in providerRunId " + runId.value()));
    }

    private AgentRun readRun(EnvironmentProfile profile, ProviderRunId runId) {
        AgentRun run = agentPort.readRun(profile, runId.sequenceId())
                .orElseThrow(() -> new UseCaseException(Reason.NOT_FOUND,
                        "No job " + runId.sequenceId() + " on the Blindata agent of environment " + profile.name()));
        if (run.context() == null) {
            throw new UseCaseException(Reason.NOT_FOUND,
                    "Job " + runId.sequenceId() + " on the Blindata agent of environment " + profile.name()
                            + " is not a contract verification job");
        }
        return run;
    }

    private String agentLog(EnvironmentProfile profile, ProviderRunId runId) {
        try {
            return agentPort.readLog(profile, runId.sequenceId()).orElse("");
        } catch (Exception e) {
            logger.warn("Agent log of job {} unavailable: {}", runId.sequenceId(), e.getMessage());
            return "(agent job log unavailable: " + e.getMessage() + ")";
        }
    }
}
