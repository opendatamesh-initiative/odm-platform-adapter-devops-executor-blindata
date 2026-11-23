package org.opendatamesh.platform.up.executor.blindata.testsupport;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/** Calls of the executor API as DevOps makes them. */
public final class ExecutorApi {

    private ExecutorApi() {
    }

    public static Response start(String environment, String tag) {
        ObjectNode body = Fixtures.JSON.createObjectNode();
        ObjectNode executorParameters = body.putObject("executorParameters");
        executorParameters.putObject("dataProductRepo").put("providerType", "GITLAB").put("externalIdentifier", "group/orders");
        executorParameters.putObject("ref").put("name", tag).put("type", "TAG");
        ObjectNode parameters = body.putObject("pipelineParameters");
        parameters.put("dataProductFqn", Fixtures.ORDERS_FQN);
        if (environment != null) {
            parameters.put("environment", environment);
        }
        parameters.put("activityName", "deploy-" + environment);
        parameters.put("taskName", "verify-contract");
        return post("start", body);
    }

    public static Response run(String verb, String providerRunId) {
        ObjectNode body = Fixtures.JSON.createObjectNode();
        body.putObject("executorParameters").putObject("ref").put("name", "v1.2.0");
        body.put("providerRunId", providerRunId);
        return post(verb, body);
    }

    private static Response post(String verb, ObjectNode body) {
        return given().contentType(ContentType.JSON).body(body.toString())
                .when().post("/api/v2/up/executor/tasks/" + verb);
    }
}
