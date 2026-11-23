package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import java.time.Instant;
import java.util.List;

/**
 * Per-rule detail of one verification run, and its verdict. It is the record of the deploy-time check: results are
 * never exported to Blindata, so this report, in the task results and logs, is the only trace of the run.
 */
public record VerificationReport(VerificationContext context, long sequenceId, String agentRunStatus,
                                 Instant startedAt, Instant finishedAt, RunStatus verdict, ReportSummary summary,
                                 List<RuleReport> rules, List<SkippedRule> skipped, List<String> notes) {

    public static final String SCHEMA = "blindata.devops.contractVerification.v1";

    public VerificationReport {
        rules = List.copyOf(rules);
        skipped = List.copyOf(skipped);
        notes = List.copyOf(notes);
    }
}
