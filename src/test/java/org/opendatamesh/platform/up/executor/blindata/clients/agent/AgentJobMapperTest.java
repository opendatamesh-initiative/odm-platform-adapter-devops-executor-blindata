package org.opendatamesh.platform.up.executor.blindata.clients.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRunStatus;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractJob;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRuleKind;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.SkippedRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationContext;
import org.opendatamesh.platform.up.executor.blindata.testsupport.Fixtures;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BDMD-5755 decision 3 (job shape, TEST RULES parity) and the statelessness check (metadata read back). */
class AgentJobMapperTest {

    private final AgentJobMapper mapper = new AgentJobMapper(Fixtures.JSON);

    /**
     * Scenario: the job is a dry-run QUALITY_PROBES job with one CONTRACT_RULE check per rule
     *   Then configs.outputOptions.dryRun is true
     *   And each check has the check code, the contract-rule envelope and the odm metadata
     */
    @Test
    void whenBuildingTheJobThenItIsADryRunWithContractRuleChecks() {
        JsonNode job = mapper.toJob(job());

        assertEquals("QUALITY_PROBES", job.get("type").asText());
        assertTrue(job.at("/configs/outputOptions/dryRun").asBoolean());
        JsonNode check = job.at("/configs/checks/0");
        assertEquals("sales - orders - order_id_not_null", check.get("code").asText());
        assertEquals("CONTRACT_RULE", check.at("/probe/type").asText());
        JsonNode query = check.at("/probe/queries/0");
        assertEquals("bq-sales", query.get("connectionName").asText());
        assertEquals("blindata.qualityProbe.contractRule.v1", query.get("schema").asText());
        assertEquals("nullValues", query.at("/rule/metric").asText());
        assertEquals("sales", query.at("/physicalBinding/schema").asText());
        assertEquals("order_id", query.at("/physicalBinding/property").asText());
        assertFalse(check.at("/odm/blocking").asBoolean(true));
        assertEquals("deploy-dev", job.at("/configs/odm/activityName").asText());
    }

    /** Scenario: the status call reads the rules, the skipped rules and the context back from the job history. */
    @Test
    void whenReadingTheHistoryThenTheJobMetadataAndResultsAreReadBack() {
        ObjectNode history = Fixtures.JSON.createObjectNode();
        history.put("sequenceId", 42).put("runStatus", "WARNING").put("startedAt", 1_700_000_000_000L)
                .put("finishedAt", "2026-11-23T10:00:00.000+00:00");
        history.putObject("jobDetails").set("configs", mapper.toJob(job()).get("configs"));
        ObjectNode results = history.putObject("jobResults");
        results.putArray("results").addObject().put("qualityCheckCode", "sales - orders - order_id_not_null")
                .put("metric", 3).put("totalElements", 100).put("score", 0);
        results.putArray("errors").addObject().put("qualityCheckCode", "x").put("error", "boom");

        AgentRun run = mapper.fromHistory(history);

        assertEquals(AgentRunStatus.WARNING, run.status());
        assertEquals(job().rules(), run.rules());
        assertEquals(job().skipped(), run.skipped());
        assertEquals(job().context(), run.context());
        assertEquals(2, run.results().size());
        assertEquals(0, run.results().get(0).score());
        assertTrue(run.results().get(1).isError());
    }

    private static ContractJob job() {
        ContractRule rule = new ContractRule("sales - orders - order_id_not_null", "order_id_not_null", "order_id_not_null",
                "urn:port", "sales", "orders", "order_id", "bq-sales", "library", false, ContractRuleKind.RULE,
                Fixtures.JSON.createObjectNode().put("type", "library").put("metric", "nullValues").put("mustBe", 0));
        SkippedRule skipped = new SkippedRule(null, null, "rule_without_id", "urn:port", "orders", null, true, "no id");
        return new ContractJob(new VerificationContext(Fixtures.ORDERS_FQN, "1.2.0", "v1.2.0", "dev", "deploy-dev",
                "verify-contract"), List.of(rule), List.of(skipped));
    }
}
