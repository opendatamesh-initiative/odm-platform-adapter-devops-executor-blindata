package org.opendatamesh.platform.up.executor.blindata.tasks.model;

/** A rule of the descriptor that is not sent to the agent, with the reason. */
public record SkippedRule(String checkCode, String ruleId, String ruleName, String portFqn, String object,
                          String property, boolean blocking, String reason) {

    public static SkippedRule of(ContractRule rule, String reason) {
        return new SkippedRule(rule.checkCode(), rule.ruleId(), rule.ruleName(), rule.portFqn(), rule.object(),
                rule.property(), rule.blocking(), reason);
    }
}
