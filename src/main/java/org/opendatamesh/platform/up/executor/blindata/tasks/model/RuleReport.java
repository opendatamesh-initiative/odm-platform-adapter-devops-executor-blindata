package org.opendatamesh.platform.up.executor.blindata.tasks.model;

/** Outcome of one rule in the verification report. */
public record RuleReport(String checkCode, String ruleId, String name, String port, String object, String property,
                         ContractRuleKind kind, boolean blocking, RuleOutcome outcome, Double metric,
                         Double totalElements, Integer score, String error, String note) {
}
