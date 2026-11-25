package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import java.util.List;

/** The verification job sent to the agent: always a dry run, carrying the rule metadata the status call needs. */
public record ContractJob(VerificationContext context, List<ContractRule> rules, List<SkippedRule> skipped) {

    public ContractJob {
        rules = List.copyOf(rules);
        skipped = List.copyOf(skipped);
    }

    public String name() {
        return "verify-contract " + context.dataProductFqn() + " " + context.dataProductVersion() + " " + context.environment();
    }
}
