package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import java.util.List;

/** The rules of a descriptor: the executable ones and those skipped while reading it. */
public record ContractRules(List<ContractRule> rules, List<SkippedRule> skipped) {

    public ContractRules {
        rules = List.copyOf(rules);
        skipped = List.copyOf(skipped);
    }
}
