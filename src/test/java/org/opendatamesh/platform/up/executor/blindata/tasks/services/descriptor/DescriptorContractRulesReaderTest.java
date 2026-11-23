package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.SkippedRule;
import org.opendatamesh.platform.up.executor.blindata.testsupport.Fixtures;
import org.opendatamesh.platform.up.executor.blindata.testsupport.TestConfigs;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BDMD-5755 decision 3 (descriptor → rules, observer parity) and decision 9 (blocking flag). */
class DescriptorContractRulesReaderTest {

    private final DescriptorContractRulesReader reader =
            new DescriptorContractRulesReader(TestConfigs.blindata(TestConfigs.valid()), Fixtures.JSON);

    /**
     * Scenario: the executable rules of the orders descriptor
     *   Given the output port "orders" declares x-blindataConnectionName "bq-sales" and library, sql and text rules
     *   When the descriptor is read
     *   Then the library and sql rules with an id are executable, with check code "sales - orders - <rule id>"
     *   And the binding is databaseSchemaName "sales", table "orders" and the column for column rules
     */
    @Test
    void whenReadingTheDescriptorThenLibraryAndSqlRulesWithIdAreExecutable() {
        ContractRules rules = read(Fixtures.ordersDescriptor());

        assertEquals(List.of("sales - orders - orders_not_empty", "sales - orders - orders_latest_partition",
                        "sales - orders - order_id_not_null", "sales - orders - order_id_unique",
                        "sales - orders - status_valid", "sales - orders - country_code_pattern"),
                rules.rules().stream().map(ContractRule::checkCode).toList());
        ContractRule notNull = rule(rules, "order_id_not_null");
        assertEquals("sales", notNull.schema());
        assertEquals("orders", notNull.object());
        assertEquals("order_id", notNull.property());
        assertEquals("bq-sales", notNull.connectionName());
        assertEquals("library", notNull.ruleType());
        assertNull(rule(rules, "orders_not_empty").property());
        assertEquals("urn:dpds:acme:dataproducts:orders:1:outputPorts:orders", notNull.portFqn());
    }

    /**
     * Scenario: rules that cannot run are skipped with a reason
     *   Then the text rule, the rule without id and the rule on a port without connection name are skipped
     *   And the quality reference (refName) is ignored
     */
    @Test
    void whenRulesCannotRunThenTheyAreSkippedWithAReason() {
        Map<String, String> reasons = read(Fixtures.ordersDescriptor()).skipped().stream()
                .collect(Collectors.toMap(s -> s.ruleId() == null ? s.ruleName() : s.ruleId(), SkippedRule::reason));

        assertEquals(3, reasons.size());
        assertTrue(reasons.get("orders_documentation").contains("type text is not executable"));
        assertTrue(reasons.get("rule_without_id").contains("no id"));
        assertTrue(reasons.get("raw_not_empty").contains("no Blindata connection name"));
    }

    /**
     * Scenario: blocking flag from customProperties
     *   Then a rule is blocking unless customProperties sets "blocking" to false, as a boolean or a string,
     *   in the ODCS property-list form or in the object form
     */
    @Test
    void whenCustomPropertiesSetBlockingFalseThenTheRuleIsNotBlocking() {
        ContractRules rules = read(Fixtures.ordersDescriptor());

        assertTrue(rule(rules, "orders_not_empty").blocking());
        assertTrue(rule(rules, "status_valid").blocking());
        assertFalse(rule(rules, "orders_latest_partition").blocking());
        assertFalse(rule(rules, "order_id_unique").blocking());
        assertFalse(rule(rules, "country_code_pattern").blocking());
    }

    /** Scenario: the rule sent to the agent has no catalog-only keys (TEST RULES parity). */
    @Test
    void whenBuildingTheExecutionRuleThenCatalogOnlyKeysAreRemoved() {
        JsonNode executionRule = rule(read(Fixtures.ordersDescriptor()), "orders_not_empty").executionRule();

        assertEquals(Fixtures.JSON.createObjectNode().put("type", "library").put("metric", "rowCount")
                .put("mustBeGreaterThan", 0), executionRule);
        JsonNode sql = rule(read(Fixtures.ordersDescriptor()), "orders_latest_partition").executionRule();
        assertFalse(sql.has("customProperties"));
        assertFalse(sql.has("id"));
    }

    /** Scenario: a check code declared twice keeps the first declaration (observer rule). */
    @Test
    void whenACheckCodeIsDeclaredTwiceThenTheSecondIsSkipped() {
        ObjectNode descriptor = (ObjectNode) Fixtures.ordersDescriptor();
        ObjectNode duplicate = Fixtures.JSON.createObjectNode().put("id", "orders_not_empty").put("type", "library")
                .put("metric", "rowCount").put("mustBeGreaterThan", 1);
        tableQuality(descriptor).add(duplicate);

        ContractRules rules = read(descriptor);

        assertEquals(1, rules.rules().stream().filter(r -> r.ruleId().equals("orders_not_empty")).count());
        assertTrue(rules.skipped().stream().anyMatch(s -> "orders_not_empty".equals(s.ruleId())
                && s.reason().contains("more than once")));
    }

    /** Scenario: a datastore API definition stored as a JSON string, with internal $ref, is read the same way. */
    @Test
    void whenTheDefinitionIsAStringWithInternalRefsThenItIsResolved() throws Exception {
        ObjectNode descriptor = (ObjectNode) Fixtures.ordersDescriptor();
        ObjectNode api = (ObjectNode) descriptor.at("/interfaceComponents/outputPorts/0/promises/api");
        ObjectNode definition = (ObjectNode) api.get("definition");
        ObjectNode table = (ObjectNode) definition.at("/schema/tables/0");
        ObjectNode components = definition.putObject("components");
        components.set("orders", table.get("definition"));
        table.putObject("definition").put("$ref", "#/components/orders");
        api.put("definition", Fixtures.JSON.writeValueAsString(definition));

        assertEquals(6, read(descriptor).rules().size());
    }

    private static com.fasterxml.jackson.databind.node.ArrayNode tableQuality(ObjectNode descriptor) {
        return (com.fasterxml.jackson.databind.node.ArrayNode) descriptor
                .at("/interfaceComponents/outputPorts/0/promises/api/definition/schema/tables/0/definition/quality");
    }

    private ContractRules read(JsonNode content) {
        return reader.read(new DataProductVersionDescriptor(Fixtures.ORDERS_FQN, "1.2.0", "v1.2.0", content));
    }

    private static ContractRule rule(ContractRules rules, String ruleId) {
        return rules.rules().stream().filter(r -> ruleId.equals(r.ruleId())).findFirst().orElseThrow();
    }
}
