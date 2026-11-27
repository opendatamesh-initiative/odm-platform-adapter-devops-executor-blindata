package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractJob;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.SkippedRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationContext;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCase;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCaseException;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCaseException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Starts the contract verification of one data product version in one environment: reads the rules from the
 * version's descriptor, drops or rejects the rules the agent cannot run there, and launches one dry-run job on the
 * agent. Nothing is exported to Blindata.
 */
class StartTask implements UseCase {

    private static final Logger logger = LoggerFactory.getLogger(StartTask.class);

    private final StartTaskCommand command;
    private final StartTaskPresenter presenter;
    private final StartTaskEnvironmentOutboundPort environmentPort;
    private final StartTaskRegistryOutboundPort registryPort;
    private final StartTaskRulesOutboundPort rulesPort;
    private final StartTaskAgentOutboundPort agentPort;

    StartTask(StartTaskCommand command, StartTaskPresenter presenter, StartTaskEnvironmentOutboundPort environmentPort,
              StartTaskRegistryOutboundPort registryPort, StartTaskRulesOutboundPort rulesPort,
              StartTaskAgentOutboundPort agentPort) {
        this.command = command;
        this.presenter = presenter;
        this.environmentPort = environmentPort;
        this.registryPort = registryPort;
        this.rulesPort = rulesPort;
        this.agentPort = agentPort;
    }

    @Override
    public void execute() {
        try {
            logger.info("Start requested: data product {} version {} tag {} in environment {} (activity {}, task {})",
                    command.dataProductFqn(), command.dataProductVersion(), command.versionTag(), command.environment(),
                    command.activityName(), command.taskName());
            validateCommand();
            EnvironmentProfile profile = findProfile();
            DataProductVersionDescriptor version = findVersion();
            ContractJob job = planJob(profile, version, rulesPort.readRules(version));
            logSkipped(job);
            long sequenceId = agentPort.launch(profile, job);
            logger.info("Started contract verification job {} of {} {} in environment {}: {} rules, {} skipped",
                    sequenceId, version.dataProductFqn(), version.versionNumber(), profile.name(),
                    job.rules().size(), job.skipped().size());
            presenter.presentStarted(new ProviderRunId(profile.name(), sequenceId));
        } catch (Exception e) {
            presenter.presentException(e);
        }
    }

    private static void logSkipped(ContractJob job) {
        for (SkippedRule skipped : job.skipped()) {
            logger.info("Rule {} skipped: {}", skipped.ruleId(), skipped.reason());
        }
    }

    private void validateCommand() {
        requireText(command.dataProductFqn(), "pipelineParameters.dataProductFqn is required");
        requireText(command.environment(), "pipelineParameters.environment is required");
        if (isBlank(command.versionTag()) && isBlank(command.dataProductVersion())) {
            throw invalid("executorParameters.ref.name (the release tag) or pipelineParameters.dataProductVersion is required");
        }
    }

    private EnvironmentProfile findProfile() {
        return environmentPort.findProfile(command.environment())
                .orElseThrow(() -> invalid("Unknown environment " + command.environment()
                        + ": no environment profile is configured with this name"));
    }

    private DataProductVersionDescriptor findVersion() {
        return registryPort.findVersion(command.dataProductFqn(), command.versionTag(), command.dataProductVersion())
                .orElseThrow(() -> invalid("Data product version not found in the Registry: " + command.dataProductFqn()
                        + (isBlank(command.dataProductVersion()) ? " with tag " + command.versionTag()
                        : " version " + command.dataProductVersion())));
    }

    /**
     * Resolves each rule's connection for the environment and checks it against what the agent can run there. A
     * blocking rule that cannot run rejects the start, because skipping it would silently weaken the gate; a
     * non-blocking one is skipped with the reason.
     */
    private ContractJob planJob(EnvironmentProfile profile, DataProductVersionDescriptor version, ContractRules contractRules) {
        List<ContractRule> runnable = new ArrayList<>();
        List<SkippedRule> skipped = new ArrayList<>(contractRules.skipped());
        List<String> rejected = new ArrayList<>();
        for (ContractRule rule : contractRules.rules()) {
            ContractRule resolved = rule.withConnectionName(profile.agentConnectionName(rule.connectionName()));
            Optional<String> unsupported = rulesPort.unsupportedReason(resolved, profile.platform());
            if (unsupported.isEmpty()) {
                runnable.add(resolved);
            } else if (resolved.blocking()) {
                rejected.add(resolved.ruleId() + " (" + resolved.portFqn() + "): " + unsupported.get());
            } else {
                skipped.add(SkippedRule.of(resolved, unsupported.get()));
            }
        }
        if (!rejected.isEmpty()) {
            throw invalid(rejected.size() + " blocking rule(s) cannot run on the Blindata agent for environment "
                    + profile.name() + " (" + profile.platform() + "): " + String.join(" | ", rejected));
        }
        return new ContractJob(context(profile, version), runnable, skipped);
    }

    private VerificationContext context(EnvironmentProfile profile, DataProductVersionDescriptor version) {
        String tag = isBlank(command.versionTag()) ? version.tag() : command.versionTag();
        return new VerificationContext(version.dataProductFqn(), version.versionNumber(), tag, profile.name(),
                blankToNull(command.activityName()), blankToNull(command.taskName()));
    }

    private static void requireText(String value, String message) {
        if (isBlank(value)) {
            throw invalid(message);
        }
    }

    private static UseCaseException invalid(String message) {
        return new UseCaseException(Reason.INVALID, message);
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
