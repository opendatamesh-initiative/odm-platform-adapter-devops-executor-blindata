package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.NotFoundException;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentCheckResult;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRunStatus;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.BlindataCredentials;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRuleKind;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.Platform;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.RunStatus;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationContext;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;
import org.opendatamesh.platform.up.executor.blindata.testsupport.Fixtures;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BDMD-5755 decisions 6 and 7: status, verdict and the results post, with fake ports. */
class ReadTaskStatusTest {

    private static final EnvironmentProfile LAB = new EnvironmentProfile("lab", "http://agent", "t", "{name}",
            Platform.GENERIC, new BlindataCredentials.ApiKey("u", "k"));
    private static final VerificationContext CONTEXT =
            new VerificationContext(Fixtures.ORDERS_FQN, "1.2.0", "v1.2.0", "lab", "deploy-lab", "verify-contract");

    private final List<VerificationReport> published = new ArrayList<>();
    private RunStatus presented;

    /** Scenario: a running job is RUNNING and nothing is posted. */
    @Test
    void whenTheJobIsRunningThenRunningAndNoPost() {
        status("lab:7", Optional.of(run(AgentRunStatus.RUNNING, CONTEXT, List.of())), false);

        assertEquals(RunStatus.RUNNING, presented);
        assertTrue(published.isEmpty());
    }

    /**
     * Scenario: an ended job with a failed blocking rule is FAILED and the report is posted first
     *   Given the agent job ended SUCCESS with score 0 on blocking rule "a"
     *   Then the report is posted to DevOps and the status is FAILED
     */
    @Test
    void whenTheJobEndedWithAFailedBlockingRuleThenTheReportIsPostedAndFailed() {
        status("lab:7", Optional.of(run(AgentRunStatus.SUCCESS, CONTEXT, List.of(new AgentCheckResult("a", 1.0, 1.0, 0, null)))), false);

        assertEquals(RunStatus.FAILED, presented);
        assertEquals(1, published.size());
        assertEquals(RunStatus.FAILED, published.get(0).verdict());
    }

    /** Scenario: a failing results post does not change the verdict. */
    @Test
    void whenTheResultsPostFailsThenTheVerdictIsStillAnswered() {
        status("lab:7", Optional.of(run(AgentRunStatus.SUCCESS, CONTEXT, List.of(new AgentCheckResult("a", 1.0, 1.0, 100, null)))), true);

        assertEquals(RunStatus.SUCCEEDED, presented);
    }

    /** Scenario: without activity and task names the report is not posted. */
    @Test
    void whenTheJobHasNoActivityNameThenTheReportIsNotPosted() {
        VerificationContext noNames = new VerificationContext(Fixtures.ORDERS_FQN, "1.2.0", "v1.2.0", "lab", null, null);
        status("lab:7", Optional.of(run(AgentRunStatus.SUCCESS, noNames, List.of(new AgentCheckResult("a", 1.0, 1.0, 100, null)))), false);

        assertEquals(RunStatus.SUCCEEDED, presented);
        assertTrue(published.isEmpty());
    }

    /** Scenario: malformed id or unknown environment → 400; unknown job or foreign job → 404. */
    @Test
    void whenTheRunCannotBeFoundThenTheErrorFollowsTheContract() {
        assertThrows(BadRequestException.class, () -> status("7", Optional.empty(), false));
        assertThrows(BadRequestException.class, () -> status("prd:7", Optional.empty(), false));
        assertThrows(NotFoundException.class, () -> status("lab:7", Optional.empty(), false));
        assertThrows(NotFoundException.class, () -> status("lab:7", Optional.of(run(AgentRunStatus.SUCCESS, null, List.of())), false));
    }

    private void status(String runId, Optional<AgentRun> run, boolean postFails) {
        new ReadTaskStatus(new ReadTaskStatusCommand(runId), (id, status) -> presented = status,
                environment -> "lab".equals(environment) ? Optional.of(LAB) : Optional.empty(),
                (profile, sequenceId) -> run,
                new ReadTaskStatusResultsOutboundPort() {
                    @Override
                    public boolean canPublish() {
                        return true;
                    }

                    @Override
                    public void publish(VerificationReport report) {
                        if (postFails) {
                            throw new IllegalStateException("DevOps down");
                        }
                        published.add(report);
                    }
                }).execute();
    }

    private static AgentRun run(AgentRunStatus status, VerificationContext context, List<AgentCheckResult> rows) {
        ContractRule rule = new ContractRule("a", "a", "a", "port", "sales", "orders", null, "c", "library", true,
                ContractRuleKind.RULE, Fixtures.JSON.createObjectNode().put("type", "library").put("metric", "rowCount"));
        return new AgentRun(7, status, status.name(), Instant.EPOCH, null, context, List.of(rule), List.of(), rows);
    }
}
