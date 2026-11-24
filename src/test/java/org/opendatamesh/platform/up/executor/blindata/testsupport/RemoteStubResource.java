package org.opendatamesh.platform.up.executor.blindata.testsupport;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Starts one {@link RemoteStub} for the whole test run and points the executor at it: Registry, DevOps, and two
 * environment profiles on the same agent: "dev" (API key, BigQuery) and "prod" (OAuth2 client credentials).
 */
public class RemoteStubResource implements QuarkusTestResourceLifecycleManager {

    public static final String LAB_USER = "executor@dev";
    public static final String LAB_API_KEY = "dev-api-key-secret";
    public static final String PRD_CLIENT_SECRET = "prod-client-secret";

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
        config.put("blindata.environments.dev.agent-url", url);
        config.put("blindata.environments.dev.tenant-uuid", "tenant-dev");
        config.put("blindata.environments.dev.platform", "BIGQUERY");
        config.put("blindata.environments.dev.auth.type", "API_KEY");
        config.put("blindata.environments.dev.auth.api-key.user", LAB_USER);
        config.put("blindata.environments.dev.auth.api-key.key", LAB_API_KEY);
        config.put("blindata.environments.prod.agent-url", url);
        config.put("blindata.environments.prod.tenant-uuid", "tenant-prod");
        config.put("blindata.environments.prod.platform", "BIGQUERY");
        config.put("blindata.environments.prod.auth.type", "OAUTH2_CLIENT_CREDENTIALS");
        config.put("blindata.environments.prod.auth.oauth2.url", url + "/oauth2/token");
        config.put("blindata.environments.prod.auth.oauth2.scope", "api://blindata/.default");
        config.put("blindata.environments.prod.auth.oauth2.client-id", "executor-prod");
        config.put("blindata.environments.prod.auth.oauth2.client-secret", PRD_CLIENT_SECRET);
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
