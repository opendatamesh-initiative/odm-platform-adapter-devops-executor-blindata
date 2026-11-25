package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCase;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCaseException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cancel of a verification run. The agent cannot stop an ad-hoc job, so a running job is left to end on its own
 * (probes take seconds to minutes) and the next status call returns its real outcome. Unknown job: not found; ended
 * job: conflict.
 */
class CancelTask implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(CancelTask.class);

    private final CancelTaskCommand command;
    private final CancelTaskPresenter presenter;
    private final CancelTaskEnvironmentOutboundPort environmentPort;
    private final CancelTaskAgentOutboundPort agentPort;

    CancelTask(CancelTaskCommand command, CancelTaskPresenter presenter,
               CancelTaskEnvironmentOutboundPort environmentPort, CancelTaskAgentOutboundPort agentPort) {
        this.command = command;
        this.presenter = presenter;
        this.environmentPort = environmentPort;
        this.agentPort = agentPort;
    }

    @Override
    public void execute() {
        try {
            ProviderRunId runId = parseRunId();
            EnvironmentProfile profile = findProfile(runId);
            AgentRun run = agentPort.readRun(profile, runId.sequenceId())
                    .orElseThrow(() -> new UseCaseException(Reason.NOT_FOUND,
                            "No job " + runId.sequenceId() + " on the Blindata agent of environment " + profile.name()));
            if (run.status().hasEnded()) {
                throw new UseCaseException(Reason.CONFLICT,
                        "Job " + runId.sequenceId() + " has already ended (" + run.rawStatus() + ")");
            }
            logger.info("Cancel of job {} accepted: the agent cannot stop an ad-hoc job, it ends on its own", runId.value());
            presenter.presentCancelAccepted(runId);
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
}
