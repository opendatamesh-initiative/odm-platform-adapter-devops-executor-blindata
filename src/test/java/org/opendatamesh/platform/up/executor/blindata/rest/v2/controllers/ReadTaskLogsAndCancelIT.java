package org.opendatamesh.platform.up.executor.blindata.rest.v2.controllers;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.testsupport.ExecutorApi;
import org.opendatamesh.platform.up.executor.blindata.testsupport.RemoteStub;
import org.opendatamesh.platform.up.executor.blindata.testsupport.RemoteStubResource;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@QuarkusTestResource(RemoteStubResource.class)
class ReadTaskLogsAndCancelIT {

    private RemoteStub stub;

    @BeforeEach
    void startJob() {
        stub = RemoteStubResource.stub();
        stub.reset();
        ExecutorApi.start("lab", "v1.2.0").then().statusCode(200);
    }

    /**
     * Scenario: the logs are the record of the run
     *   Given the agent job ended with a failed blocking rule
     *   Then the logs show the verdict, one line per rule with outcome and blocking flag, the skipped rules
     *   with their reason, and the agent job log
     */
    @Test
    void whenReadingTheLogsThenTheReportTableAndTheAgentLogAreReturned() {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("sales - orders - orders_not_empty", 100);
        scores.put("sales - orders - orders_latest_partition", 100);
        scores.put("sales - orders - order_id_not_null", 0);
        scores.put("sales - orders - order_id_unique", 100);
        scores.put("sales - orders - status_valid", 100);
        stub.history("SUCCESS", scores, Map.of());

        ExecutorApi.run("logs", "lab:42").then().statusCode(200)
                .body("content", containsString("Contract verification: FAILED"))
                .body("content", containsString("FAILED   yes"))
                .body("content", containsString("sales - orders - order_id_not_null"))
                .body("content", containsString("country_code_pattern"))
                .body("content", containsString("REGEXP_CONTAINS"))
                .body("content", containsString("agent: launching job\nagent: 5 checks executed"))
                .body("generatedAt", notNullValue());
    }

    /** Scenario: cancel of a running job is accepted (the agent cannot stop it); of an ended job 409; unknown 404. */
    @Test
    void whenCancelingThenRunningIsAcceptedEndedConflictsUnknownIsNotFound() {
        stub.history("RUNNING", Map.of(), Map.of());
        ExecutorApi.run("cancel", "lab:42").then().statusCode(200);
        stub.history("SUCCESS", Map.of(), Map.of());
        ExecutorApi.run("cancel", "lab:42").then().statusCode(409).body(containsString("already ended"));
        ExecutorApi.run("cancel", "lab:43").then().statusCode(404);
    }
}
