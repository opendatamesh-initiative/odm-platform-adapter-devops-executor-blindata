package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The verdict policy of a contract verification, shared by the status and logs use cases.
 *
 * <ul>
 *   <li>The agent's run status is not the verdict: a failed rule (score 0) does not fail the job.</li>
 *   <li>A rule passes with a score above 0, fails with score 0, and is an error when the agent reports an error, no
 *   score, or no row at all.</li>
 *   <li>The task fails when a blocking rule fails or errors (fail-closed); structural checks are always blocking.</li>
 *   <li>A job the agent stopped is canceled; a job that crashed fails.</li>
 * </ul>
 */
public final class ContractVerdict {

    private static final String INVALID_VALUES = "invalidValues";

    private ContractVerdict() {
    }

    public static VerificationReport evaluate(AgentRun run) {
        Map<String, AgentCheckResult> rowsByCode = rowsByCode(run.results());
        List<RuleReport> rules = new ArrayList<>();
        for (ContractRule rule : run.rules()) {
            rules.add(reportRule(rule, rowsByCode.get(rule.checkCode())));
        }
        ReportSummary summary = summarize(rules, run.skipped());
        RunStatus verdict = verdict(run.status(), summary);
        return new VerificationReport(run.context(), run.sequenceId(), run.rawStatus(), run.startedAt(),
                run.finishedAt(), verdict, summary, rules, run.skipped(), notes(run, rules));
    }

    public static RunStatus verdict(AgentRunStatus status, ReportSummary summary) {
        return switch (status) {
            case RUNNING, UNKNOWN -> RunStatus.RUNNING;
            case STOPPED -> RunStatus.CANCELED;
            case ERROR -> RunStatus.FAILED;
            case SUCCESS, WARNING -> summary.failedBlocking() > 0 ? RunStatus.FAILED : RunStatus.SUCCEEDED;
        };
    }

    private static RuleReport reportRule(ContractRule rule, AgentCheckResult row) {
        RuleOutcome outcome = outcome(row);
        String error = row == null ? "no result returned by the agent" : errorOf(row);
        return new RuleReport(rule.checkCode(), rule.ruleId(), rule.ruleName(), rule.portFqn(), rule.object(),
                rule.property(), rule.kind(), isBlocking(rule), outcome,
                row == null ? null : row.metric(), row == null ? null : row.totalElements(),
                row == null ? null : row.score(), error, note(rule, outcome));
    }

    private static RuleOutcome outcome(AgentCheckResult row) {
        if (row == null || row.isError() || row.score() == null) {
            return RuleOutcome.ERROR;
        }
        return row.score() > 0 ? RuleOutcome.PASSED : RuleOutcome.FAILED;
    }

    private static String errorOf(AgentCheckResult row) {
        if (row.isError()) {
            return row.error();
        }
        return row.score() == null ? "the agent returned no score for this rule" : null;
    }

    private static String note(ContractRule rule, RuleOutcome outcome) {
        if (outcome == RuleOutcome.FAILED && INVALID_VALUES.equals(rule.metric())) {
            return "invalidValues counts NULL values as invalid";
        }
        return null;
    }

    private static boolean isBlocking(ContractRule rule) {
        return rule.kind() == ContractRuleKind.STRUCTURE || rule.blocking();
    }

    private static ReportSummary summarize(List<RuleReport> rules, List<SkippedRule> skipped) {
        int passed = 0;
        int failed = 0;
        int error = 0;
        int failedBlocking = 0;
        for (RuleReport rule : rules) {
            switch (rule.outcome()) {
                case PASSED -> passed++;
                case FAILED -> failed++;
                case ERROR -> error++;
                case SKIPPED -> { }
            }
            if (rule.blocking() && (rule.outcome() == RuleOutcome.FAILED || rule.outcome() == RuleOutcome.ERROR)) {
                failedBlocking++;
            }
        }
        return new ReportSummary(rules.size() + skipped.size(), passed, failed, error, skipped.size(), failedBlocking);
    }

    private static List<String> notes(AgentRun run, List<RuleReport> rules) {
        List<String> notes = new ArrayList<>();
        if (rules.isEmpty()) {
            notes.add("No executable rules: nothing was checked on the data.");
        }
        if (run.status() == AgentRunStatus.ERROR) {
            notes.add("The agent job ended in ERROR: the task fails whatever the rule outcomes.");
        }
        if (run.status() == AgentRunStatus.STOPPED) {
            notes.add("The agent job was stopped: the task is canceled.");
        }
        return notes;
    }

    private static Map<String, AgentCheckResult> rowsByCode(List<AgentCheckResult> rows) {
        Map<String, AgentCheckResult> byCode = new HashMap<>();
        for (AgentCheckResult row : rows) {
            if (row.checkCode() != null) {
                byCode.merge(row.checkCode(), row, ContractVerdict::worse);
            }
        }
        return byCode;
    }

    private static AgentCheckResult worse(AgentCheckResult first, AgentCheckResult second) {
        return first.isError() ? first : second;
    }
}
