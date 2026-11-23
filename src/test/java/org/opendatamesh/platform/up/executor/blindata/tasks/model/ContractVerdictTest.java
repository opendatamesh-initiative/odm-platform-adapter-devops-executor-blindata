package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.testsupport.Fixtures;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BDMD-5755 decision 7: per-rule outcomes and verdict, computed from the rules, not from the agent run status. */
class ContractVerdictTest {

    private static final VerificationContext CONTEXT =
            new VerificationContext(Fixtures.ORDERS_FQN, "1.2.0", "v1.2.0", "lab", "deploy-lab", "verify-contract");

    /** Scenario: all rules pass → SUCCEEDED. */
    @Test
    void whenAllRulesPassThenSucceeded() {
        VerificationReport report = evaluate(AgentRunStatus.SUCCESS, List.of(rule("a", true)), List.of(row("a", 100, null)));

        assertEquals(RunStatus.SUCCEEDED, report.verdict());
        assertEquals(RuleOutcome.PASSED, report.rules().get(0).outcome());
    }

    /**
     * Scenario: a failed blocking rule fails the task although the agent job succeeded
     *   Given the agent job ended SUCCESS and blocking rule "a" has score 0
     *   Then the verdict is FAILED with one blocking failure
     */
    @Test
    void whenABlockingRuleFailsThenFailedEvenIfTheJobSucceeded() {
        VerificationReport report = evaluate(AgentRunStatus.SUCCESS, List.of(rule("a", true), rule("b", true)),
                List.of(row("a", 0, null), row("b", 100, null)));

        assertEquals(RunStatus.FAILED, report.verdict());
        assertEquals(1, report.summary().failedBlocking());
    }

    /** Scenario: a failed non-blocking rule is reported and does not fail the task. */
    @Test
    void whenOnlyANonBlockingRuleFailsThenSucceeded() {
        VerificationReport report = evaluate(AgentRunStatus.WARNING, List.of(rule("a", false)), List.of(row("a", 0, null)));

        assertEquals(RunStatus.SUCCEEDED, report.verdict());
        assertEquals(1, report.summary().failed());
    }

    /** Scenario: an error row, a missing score or no row at all on a blocking rule fails closed. */
    @Test
    void whenABlockingRuleErrorsOrHasNoResultThenFailed() {
        assertEquals(RunStatus.FAILED, evaluate(AgentRunStatus.SUCCESS, List.of(rule("a", true)),
                List.of(row("a", null, "table not found"))).verdict());
        assertEquals(RunStatus.FAILED, evaluate(AgentRunStatus.SUCCESS, List.of(rule("a", true)),
                List.of(row("a", null, null))).verdict());
        VerificationReport noRow = evaluate(AgentRunStatus.SUCCESS, List.of(rule("a", true)), List.of());
        assertEquals(RunStatus.FAILED, noRow.verdict());
        assertEquals("no result returned by the agent", noRow.rules().get(0).error());
    }

    /** Scenario: agent status mapping: RUNNING and unknown → RUNNING, STOPPED → CANCELED, ERROR → FAILED. */
    @Test
    void whenTheAgentJobIsNotSuccessfulThenTheStatusFollowsTheMapping() {
        assertEquals(RunStatus.RUNNING, evaluate(AgentRunStatus.RUNNING, List.of(rule("a", true)), List.of()).verdict());
        assertEquals(RunStatus.RUNNING, evaluate(AgentRunStatus.UNKNOWN, List.of(rule("a", true)), List.of()).verdict());
        assertEquals(RunStatus.CANCELED, evaluate(AgentRunStatus.STOPPED, List.of(rule("a", true)), List.of()).verdict());
        assertEquals(RunStatus.FAILED, evaluate(AgentRunStatus.ERROR, List.of(rule("a", false)),
                List.of(row("a", 100, null))).verdict());
    }

    /** Scenario: no executable rule → SUCCEEDED with a note. */
    @Test
    void whenThereAreNoRulesThenSucceededWithANote() {
        VerificationReport report = evaluate(AgentRunStatus.SUCCESS, List.of(), List.of());

        assertEquals(RunStatus.SUCCEEDED, report.verdict());
        assertTrue(report.notes().get(0).contains("No executable rules"));
    }

    /** Scenario: a failed invalidValues rule notes that NULL values count as invalid. */
    @Test
    void whenAnInvalidValuesRuleFailsThenTheReportNotesNulls() {
        ContractRule rule = new ContractRule("sales - orders - s", "s", "s", "port", "sales", "orders", "status", "c",
                "library", true, ContractRuleKind.RULE,
                Fixtures.JSON.createObjectNode().put("type", "library").put("metric", "invalidValues"));

        VerificationReport report = evaluate(AgentRunStatus.SUCCESS, List.of(rule), List.of(row("sales - orders - s", 0, null)));

        assertTrue(report.rules().get(0).note().contains("NULL"));
    }

    private static VerificationReport evaluate(AgentRunStatus status, List<ContractRule> rules, List<AgentCheckResult> rows) {
        return ContractVerdict.evaluate(new AgentRun(42, status, status.name(), Instant.EPOCH, Instant.EPOCH, CONTEXT,
                rules, List.of(), rows));
    }

    private static ContractRule rule(String code, boolean blocking) {
        return new ContractRule(code, code, code, "port", "sales", "orders", null, "conn", "library", blocking,
                ContractRuleKind.RULE, Fixtures.JSON.createObjectNode().put("type", "library").put("metric", "rowCount"));
    }

    private static AgentCheckResult row(String code, Integer score, String error) {
        return new AgentCheckResult(code, 1.0, 10.0, score, error);
    }
}
