package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import java.time.Instant;
import java.util.List;

/**
 * A verification job as read back from the agent history: its status and results, plus the job it was started with
 * (context, rules, skipped rules), which the executor stored in the job itself.
 */
public record AgentRun(long sequenceId, AgentRunStatus status, String rawStatus, Instant startedAt, Instant finishedAt,
                       VerificationContext context, List<ContractRule> rules, List<SkippedRule> skipped,
                       List<AgentCheckResult> results) {

    public AgentRun {
        rules = List.copyOf(rules);
        skipped = List.copyOf(skipped);
        results = List.copyOf(results);
    }
}
