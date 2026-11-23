package org.opendatamesh.platform.up.executor.blindata.clients.agent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRuleKind;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.Platform;
import org.opendatamesh.platform.up.executor.blindata.testsupport.Fixtures;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** BDMD-5755 decision 13: the agent's SQL limits, checked at start. */
class AgentRuleSupportTest {

    /**
     * Scenario: invalidValues with a pattern on BigQuery
     *   Given a library invalidValues rule with arguments.pattern
     *   Then it is unsupported on a BIGQUERY profile, with the sql rule to write instead
     *   And it is supported on a GENERIC profile
     */
    @Test
    void whenInvalidValuesHasAPatternThenItIsUnsupportedOnBigQueryOnly() {
        ObjectNode rule = library("invalidValues");
        rule.putObject("arguments").put("pattern", "^[A-Z]{2}$");

        Optional<String> reason = AgentRuleSupport.unsupportedReason(rule(rule, "sales", "orders", "country_code"), Platform.BIGQUERY);

        assertTrue(reason.orElseThrow().contains("REGEXP_CONTAINS"));
        assertTrue(AgentRuleSupport.unsupportedReason(rule(rule, "sales", "orders", "country_code"), Platform.GENERIC).isEmpty());
    }

    /** Scenario: identifiers outside [a-zA-Z0-9_]+ (a hyphenated GCP project) are unsupported on any platform. */
    @Test
    void whenAnIdentifierIsNotPlainThenTheRuleIsUnsupported() {
        Optional<String> reason = AgentRuleSupport.unsupportedReason(
                rule(library("rowCount"), "my-project.sales", "orders", null), Platform.GENERIC);

        assertTrue(reason.orElseThrow().contains("not a plain identifier"));
    }

    /** Scenario: an unknown library metric is unsupported. */
    @Test
    void whenTheLibraryMetricIsUnknownThenTheRuleIsUnsupported() {
        assertTrue(AgentRuleSupport.unsupportedReason(rule(library("freshness"), "sales", "orders", null), Platform.GENERIC)
                .orElseThrow().contains("freshness"));
    }

    /** Scenario: a sql rule with its own table reference needs no binding; one with {object} does. */
    @Test
    void whenASqlRuleUsesTheObjectPlaceholderThenItNeedsSchemaAndObject() {
        ObjectNode withPlaceholder = Fixtures.JSON.createObjectNode().put("type", "sql").put("query", "SELECT COUNT(*) FROM {object}");
        ObjectNode withoutPlaceholder = Fixtures.JSON.createObjectNode().put("type", "sql").put("query", "SELECT 1");

        assertTrue(AgentRuleSupport.unsupportedReason(sql(withPlaceholder), Platform.BIGQUERY).isPresent());
        assertTrue(AgentRuleSupport.unsupportedReason(sql(withoutPlaceholder), Platform.BIGQUERY).isEmpty());
    }

    private static ObjectNode library(String metric) {
        return Fixtures.JSON.createObjectNode().put("type", "library").put("metric", metric).put("mustBe", 0);
    }

    private static ContractRule sql(ObjectNode rule) {
        return new ContractRule("c", "r", "r", "port", null, null, null, "conn", "sql", true, ContractRuleKind.RULE, rule);
    }

    private static ContractRule rule(ObjectNode rule, String schema, String object, String property) {
        return new ContractRule("c", "r", "r", "port", schema, object, property, "conn", "library", true,
                ContractRuleKind.RULE, rule);
    }
}
