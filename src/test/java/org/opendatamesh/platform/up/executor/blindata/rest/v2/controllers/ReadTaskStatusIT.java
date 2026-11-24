package org.opendatamesh.platform.up.executor.blindata.rest.v2.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.testsupport.ExecutorApi;
import org.opendatamesh.platform.up.executor.blindata.testsupport.Fixtures;
import org.opendatamesh.platform.up.executor.blindata.testsupport.RemoteStub;
import org.opendatamesh.platform.up.executor.blindata.testsupport.RemoteStubResource;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(RemoteStubResource.class)
class ReadTaskStatusIT {

    private static final String RESULTS = "/api/v2/pp/devops/activities/tasks/results";
    private static final Map<String, Integer> ALL_PASS = Map.of(
            "sales - orders - orders_not_empty", 100, "sales - orders - orders_latest_partition", 100,
            "sales - orders - order_id_not_null", 100, "sales - orders - order_id_unique", 100,
            "sales - orders - status_valid", 100);

    private RemoteStub stub;

    @BeforeEach
    void startJob() {
        stub = RemoteStubResource.stub();
        stub.reset();
        ExecutorApi.start("dev", "v1.2.0").then().statusCode(200);
    }

    /** Scenario: while the agent job runs, the status is RUNNING and nothing is posted to DevOps. */
    @Test
    void whenTheJobRunsThenRunning() {
        stub.history("RUNNING", Map.of(), Map.of());

        ExecutorApi.run("status", "dev:42").then().statusCode(200)
                .body("status", equalTo("RUNNING")).body("providerRunId", equalTo("dev:42"));
        assertTrue(stub.requests("POST", RESULTS).isEmpty());
    }

    /** Scenario: every rule passes → SUCCEEDED, and the report is posted to the task's results. */
    @Test
    void whenAllRulesPassThenSucceededAndTheReportIsPosted() {
        stub.history("SUCCESS", ALL_PASS, Map.of());

        ExecutorApi.run("status", "dev:42").then().statusCode(200).body("status", equalTo("SUCCEEDED"));

        JsonNode post = stub.lastRequest("POST", RESULTS).json();
        assertEquals(Fixtures.ORDERS_FQN, post.get("dataProductFqn").asText());
        assertEquals("v1.2.0", post.get("dataProductVersionTag").asText());
        assertEquals("deploy-dev", post.get("activityName").asText());
        assertEquals("verify-contract", post.get("taskName").asText());
    }

    /**
     * Scenario: a blocking rule fails although the agent job succeeded → FAILED, with the per-rule report in DevOps
     *   Given the agent job ended SUCCESS with score 0 on blocking rule "order_id_not_null"
     *   Then the status is FAILED
     *   And the report posted to DevOps says FAILED, with one blocking failure and the rule outcome
     */
    @Test
    void whenABlockingRuleFailsThenFailedAndTheReportSaysWhy() throws Exception {
        Map<String, Integer> scores = new java.util.HashMap<>(ALL_PASS);
        scores.put("sales - orders - order_id_not_null", 0);
        stub.history("SUCCESS", scores, Map.of());

        ExecutorApi.run("status", "dev:42").then().statusCode(200).body("status", equalTo("FAILED"));

        JsonNode report = Fixtures.JSON.readTree(stub.lastRequest("POST", RESULTS).json().at("/results/0/content").asText());
        assertEquals("FAILED", report.get("verdict").asText());
        assertEquals(1, report.at("/summary/failedBlocking").asInt());
        assertEquals(4, report.at("/summary/skipped").asInt());
        assertTrue(report.get("rules").toString().contains("\"outcome\":\"FAILED\""));
    }

    /** Scenario: a failed non-blocking rule does not fail the task. */
    @Test
    void whenOnlyANonBlockingRuleFailsThenSucceeded() {
        Map<String, Integer> scores = new java.util.HashMap<>(ALL_PASS);
        scores.put("sales - orders - order_id_unique", 0);
        stub.history("WARNING", scores, Map.of());

        ExecutorApi.run("status", "dev:42").then().statusCode(200).body("status", equalTo("SUCCEEDED"));
    }

    /** Scenario: an error row on a blocking rule (e.g. table not found) fails closed. */
    @Test
    void whenABlockingRuleErrorsThenFailed() {
        Map<String, Integer> scores = new java.util.HashMap<>(ALL_PASS);
        scores.remove("sales - orders - orders_not_empty");
        stub.history("SUCCESS", scores, Map.of("sales - orders - orders_not_empty", "Table sales.orders not found"));

        ExecutorApi.run("status", "dev:42").then().statusCode(200).body("status", equalTo("FAILED"));
    }

    /** Scenario: a stopped agent job is CANCELED; a crashed one is FAILED. */
    @Test
    void whenTheJobIsStoppedOrCrashedThenCanceledOrFailed() {
        stub.history("STOPPED", Map.of(), Map.of());
        ExecutorApi.run("status", "dev:42").then().statusCode(200).body("status", equalTo("CANCELED"));
        stub.history("ERROR", ALL_PASS, Map.of());
        ExecutorApi.run("status", "dev:42").then().statusCode(200).body("status", equalTo("FAILED"));
    }

    /** Scenario: a DevOps refusal of the results does not change the verdict. */
    @Test
    void whenDevOpsRefusesTheResultsThenTheVerdictIsStillAnswered() {
        stub.reply("POST", RESULTS, 400, "{\"message\":\"task not running\"}");
        stub.history("SUCCESS", ALL_PASS, Map.of());

        ExecutorApi.run("status", "dev:42").then().statusCode(200).body("status", equalTo("SUCCEEDED"));
    }

    /** Scenario: malformed id → 400, unknown environment → 400, unknown job → 404. */
    @Test
    void whenTheRunIdIsWrongThenTheErrorFollowsTheContract() {
        ExecutorApi.run("status", "42").then().statusCode(400);
        ExecutorApi.run("status", "staging:42").then().statusCode(400);
        ExecutorApi.run("status", "dev:43").then().statusCode(404);
    }
}
