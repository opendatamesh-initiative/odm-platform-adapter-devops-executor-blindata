package org.opendatamesh.platform.up.executor.blindata.testsupport;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Starts one {@link RemoteStub} for the whole test run and points the executor at it: Registry, DevOps, and two
 * environment profiles on the same agent: "lab" (API key, BigQuery) and "prd" (OAuth2 client credentials).
 */
public class RemoteStubResource implements QuarkusTestResourceLifecycleManager {

    public static final String LAB_USER = "executor@lab";
    public static final String LAB_API_KEY = "lab-api-key-secret";
    public static final String PRD_CLIENT_SECRET = "prd-client-secret";

    private static volatile RemoteStub stub;

    public static RemoteStub stub() {
        RemoteStub current = stub;
        if (current == null) {
            throw new IllegalStateException("RemoteStubResource is not running");
        }
        return current;
    }

    @Override
    public Map<String, String> start() {
        try {
            stub = new RemoteStub();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start the remote stub", e);
        }
        String url = stub.baseUrl();
        Map<String, String> config = new HashMap<>();
        config.put("odm.product-plane.registry-service.address", url);
        config.put("odm.product-plane.devops-service.active", "true");
        config.put("odm.product-plane.devops-service.address", url);
        config.put("blindata.environments.lab.agent-url", url);
        config.put("blindata.environments.lab.tenant-uuid", "tenant-lab");
        config.put("blindata.environments.lab.platform", "BIGQUERY");
        config.put("blindata.environments.lab.auth.type", "API_KEY");
        config.put("blindata.environments.lab.auth.api-key.user", LAB_USER);
        config.put("blindata.environments.lab.auth.api-key.key", LAB_API_KEY);
        config.put("blindata.environments.prd.agent-url", url);
        config.put("blindata.environments.prd.tenant-uuid", "tenant-prd");
        config.put("blindata.environments.prd.platform", "BIGQUERY");
        config.put("blindata.environments.prd.auth.type", "OAUTH2_CLIENT_CREDENTIALS");
        config.put("blindata.environments.prd.auth.oauth2.url", url + "/oauth2/token");
        config.put("blindata.environments.prd.auth.oauth2.scope", "api://blindata/.default");
        config.put("blindata.environments.prd.auth.oauth2.client-id", "executor-prd");
        config.put("blindata.environments.prd.auth.oauth2.client-secret", PRD_CLIENT_SECRET);
        return config;
    }

    @Override
    public void stop() {
        RemoteStub current = stub;
        stub = null;
        if (current != null) {
            current.close();
        }
    }
}
