package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractVerdict;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ReportSummary;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.RuleOutcome;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.RuleReport;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.RunStatus;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCase;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCaseException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the verification job and answers its status. When the job has ended, the verdict comes from the rule outcomes
 * (not from the agent's run status), and the report is posted to DevOps before the terminal status is answered,
 * while DevOps still accepts results for the task. A failed post never changes the verdict: the report stays in the
 * task logs.
 */
class ReadTaskStatus implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(ReadTaskStatus.class);

    private final ReadTaskStatusCommand command;
    private final ReadTaskStatusPresenter presenter;
    private final ReadTaskStatusEnvironmentOutboundPort environmentPort;
    private final ReadTaskStatusAgentOutboundPort agentPort;
    private final ReadTaskStatusResultsOutboundPort resultsPort;

    ReadTaskStatus(ReadTaskStatusCommand command, ReadTaskStatusPresenter presenter,
                   ReadTaskStatusEnvironmentOutboundPort environmentPort, ReadTaskStatusAgentOutboundPort agentPort,
                   ReadTaskStatusResultsOutboundPort resultsPort) {
        this.command = command;
        this.presenter = presenter;
        this.environmentPort = environmentPort;
        this.agentPort = agentPort;
        this.resultsPort = resultsPort;
    }

    @Override
    public void execute() {
        try {
            ProviderRunId runId = parseRunId();
            AgentRun run = readRun(findProfile(runId), runId);
            VerificationReport report = ContractVerdict.evaluate(run);
            if (report.verdict().isTerminal()) {
                logVerdict(runId, report);
                publishReport(report);
            } else {
                logger.debug("Run {} still running (agent status {})", runId.value(), run.rawStatus());
            }
            presenter.presentStatus(runId.value(), report.verdict());
        } catch (Exception e) {
            presenter.presentException(e);
        }
    }

    private static void logVerdict(ProviderRunId runId, VerificationReport report) {
        ReportSummary summary = report.summary();
        logger.info("Run {} of {} {} ended {} (agent status {}): {} rules, {} passed, {} failed, {} error, {} skipped, "
                        + "{} blocking failed or errored",
                runId.value(), report.context().dataProductFqn(), report.context().dataProductVersion(), report.verdict(),
                report.agentRunStatus(), summary.total(), summary.passed(), summary.failed(), summary.error(),
                summary.skipped(), summary.failedBlocking());
        for (String note : report.notes()) {
            logger.info("Run {}: {}", runId.value(), note);
        }
        for (RuleReport rule : report.rules()) {
            if (rule.outcome() == RuleOutcome.ERROR) {
                logger.warn("Run {}: rule {} ({}) errored: {}", runId.value(), rule.ruleId(), rule.port(), rule.error());
            } else if (rule.outcome() == RuleOutcome.FAILED) {
                logger.info("Run {}: rule {} ({}) failed{}", runId.value(), rule.ruleId(), rule.port(),
                        rule.blocking() ? " (blocking)" : "");
            }
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

    private void publishReport(VerificationReport report) {
        if (!resultsPort.canPublish()) {
            return;
        }
        if (report.context().activityName() == null || report.context().taskName() == null) {
            logger.info("Report of job {} not posted to DevOps: activityName and taskName are not set; it stays in the task logs",
                    report.sequenceId());
            return;
        }
        try {
            resultsPort.publish(report);
            logger.info("Report of job {} posted to DevOps", report.sequenceId());
        } catch (Exception e) {
            logger.warn("Report of job {} not posted to DevOps, it stays in the task logs: {}", report.sequenceId(), e.getMessage());
        }
    }
}
