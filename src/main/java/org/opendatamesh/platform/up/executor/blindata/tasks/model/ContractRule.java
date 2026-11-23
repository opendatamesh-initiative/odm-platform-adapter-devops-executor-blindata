package org.opendatamesh.platform.up.executor.blindata.tasks.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One executable contract rule of the descriptor ({@code library} or {@code sql}), bound to a table or a column.
 *
 * @param checkCode      {@code <domain> - <name> - <rule id>}, the observer's check code
 * @param connectionName the connection name the agent resolves (descriptor name after the environment mapping)
 * @param executionRule  the ODCS rule without its catalog-only keys, as the agent runs it
 */
public record ContractRule(String checkCode, String ruleId, String ruleName, String portFqn,
                           String schema, String object, String property, String connectionName,
                           String ruleType, boolean blocking, ContractRuleKind kind, JsonNode executionRule) {

    public static final String TYPE_LIBRARY = "library";
    public static final String TYPE_SQL = "sql";

    public String metric() {
        JsonNode metric = executionRule == null ? null : executionRule.get("metric");
        return metric != null && metric.isTextual() ? metric.asText() : null;
    }

    public ContractRule withConnectionName(String agentConnectionName) {
        return new ContractRule(checkCode, ruleId, ruleName, portFqn, schema, object, property, agentConnectionName,
                ruleType, blocking, kind, executionRule);
    }
}
