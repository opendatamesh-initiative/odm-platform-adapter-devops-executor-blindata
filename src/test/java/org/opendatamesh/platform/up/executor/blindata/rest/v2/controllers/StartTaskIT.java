package org.opendatamesh.platform.up.executor.blindata.rest.v2.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.testsupport.ExecutorApi;
import org.opendatamesh.platform.up.executor.blindata.testsupport.RemoteStub;
import org.opendatamesh.platform.up.executor.blindata.testsupport.RemoteStubResource;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(RemoteStubResource.class)
class StartTaskIT {

    private static final String RUNNER = "/agent-api/v1/jobs/runner/async";

    private RemoteStub stub;

    @BeforeEach
    void resetStub() {
        stub = RemoteStubResource.stub();
        stub.reset();
    }

    /**
     * Feature: Start a contract verification (BDMD-5755)
     *
     * Scenario: start runs the descriptor rules on the agent of the environment, in dry run
     *   Given version v1.2.0 of the orders data product in the Registry
     *   When DevOps starts the task with environment "lab" and ref "v1.2.0"
     *   Then the agent of profile "lab" receives a dry-run QUALITY_PROBES job with the lab tenant and API key
     *   And the non-blocking pattern rule is skipped on BigQuery
     *   And the providerRunId is "lab:42"
     */
    @Test
    void whenStartingInLabThenTheAgentRunsADryRunJobWithTheLabCredentials() {
        ExecutorApi.start("lab", "v1.2.0").then().statusCode(200).body("providerRunId", equalTo("lab:42"));

        RemoteStub.Recorded launch = stub.lastRequest("POST", RUNNER);
        assertEquals("tenant-lab", launch.header("X-BD-Tenant"));
        assertEquals(RemoteStubResource.LAB_USER, launch.header("X-BD-User"));
        assertEquals(RemoteStubResource.LAB_API_KEY, launch.header("X-BD-ApiKey"));
        JsonNode job = launch.json();
        assertEquals("QUALITY_PROBES", job.get("type").asText());
        assertTrue(job.at("/configs/outputOptions/dryRun").asBoolean());
        assertEquals(5, job.at("/configs/checks").size());
        assertEquals("sales - orders - orders_not_empty", job.at("/configs/checks/0/code").asText());
        assertTrue(job.at("/configs/skipped").toString().contains("country_code_pattern"));
    }

    /**
     * Scenario: an OAuth2 profile authenticates with a bearer token from the identity provider
     *   When DevOps starts the task with environment "prd"
     *   Then the executor asks the token endpoint with client credentials
     *   And the agent receives "Authorization: Bearer <token>" and the prd tenant, and no API key
     */
    @Test
    void whenStartingInPrdThenTheAgentReceivesTheOAuth2BearerToken() {
        ExecutorApi.start("prd", "v1.2.0").then().statusCode(200).body("providerRunId", equalTo("prd:42"));

        RemoteStub.Recorded launch = stub.lastRequest("POST", RUNNER);
        assertEquals("Bearer " + RemoteStub.TOKEN, launch.header("Authorization"));
        assertEquals("tenant-prd", launch.header("X-BD-Tenant"));
        assertEquals(null, launch.header("X-BD-ApiKey"));
    }

    /** Scenario: a blocking rule BigQuery cannot run rejects the start with 400 and the sql to write; nothing runs. */
    @Test
    void whenABlockingRuleCannotRunOnBigQueryThenBadRequestAndNoJob() {
        ExecutorApi.start("lab", "v1.3.0").then().statusCode(400)
                .body(containsString("country_code_pattern"), containsString("REGEXP_CONTAINS"));

        assertTrue(stub.requests("POST", RUNNER).isEmpty());
    }

    /** Scenario: unknown environment, unknown version, missing environment → 400 with a clear message. */
    @Test
    void whenTheRequestCannotBeResolvedThenBadRequest() {
        ExecutorApi.start("staging", "v1.2.0").then().statusCode(400).body(containsString("Unknown environment staging"));
        ExecutorApi.start("lab", "v9.9.9").then().statusCode(400).body(containsString("not found in the Registry"));
        ExecutorApi.start(null, "v1.2.0").then().statusCode(400).body(containsString("pipelineParameters.environment"));
    }

    /** Scenario: the agent refuses the credentials → 502, and the message carries no secret. */
    @Test
    void whenTheAgentRefusesTheCredentialsThenBadGatewayWithoutSecrets() {
        stub.reply("POST", RUNNER, 401, "{\"error\":\"unauthorized\"}");

        ExecutorApi.start("lab", "v1.2.0").then().statusCode(502)
                .body(containsString("refused the credentials"), not(containsString(RemoteStubResource.LAB_API_KEY)));
    }
}
