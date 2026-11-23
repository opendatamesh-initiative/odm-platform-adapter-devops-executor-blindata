package org.opendatamesh.platform.up.executor.blindata.testsupport;

import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.configuration.MemorySizeConverter;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties;
import org.opendatamesh.platform.up.executor.blindata.config.OdmServicesProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Builds the typed configuration from a property map, without starting the application. */
public final class TestConfigs {

    private TestConfigs() {
    }

    /** A valid configuration: Registry, DevOps, profile "lab" (API key, BigQuery) and "prd" (OAuth2 secret). */
    public static Map<String, String> valid() {
        Map<String, String> properties = new HashMap<>();
        properties.put("odm.product-plane.registry-service.active", "true");
        properties.put("odm.product-plane.registry-service.address", "http://registry:8080");
        properties.put("odm.product-plane.devops-service.active", "true");
        properties.put("odm.product-plane.devops-service.address", "http://devops:8080");
        properties.put("blindata.environments.lab.agent-url", "http://agent:8082");
        properties.put("blindata.environments.lab.tenant-uuid", "tenant-lab");
        properties.put("blindata.environments.lab.platform", "BIGQUERY");
        properties.put("blindata.environments.lab.auth.type", "API_KEY");
        properties.put("blindata.environments.lab.auth.api-key.user", "executor@lab");
        properties.put("blindata.environments.lab.auth.api-key.key", "lab-key");
        properties.put("blindata.environments.prd.agent-url", "http://agent:8082");
        properties.put("blindata.environments.prd.tenant-uuid", "tenant-prd");
        properties.put("blindata.environments.prd.auth.type", "OAUTH2_CLIENT_CREDENTIALS");
        properties.put("blindata.environments.prd.auth.oauth2.url", "http://idp/token");
        properties.put("blindata.environments.prd.auth.oauth2.scope", "api://blindata/.default");
        properties.put("blindata.environments.prd.auth.oauth2.client-id", "executor");
        properties.put("blindata.environments.prd.auth.oauth2.client-secret", "prd-secret");
        return properties;
    }

    public static BlindataProperties blindata(Map<String, String> properties) {
        return config(properties).getConfigMapping(BlindataProperties.class);
    }

    public static OdmServicesProperties odm(Map<String, String> properties) {
        return config(properties).getConfigMapping(OdmServicesProperties.class);
    }

    private static SmallRyeConfig config(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withConverter(Duration.class, 200, new DurationConverter())
                .withConverter(MemorySize.class, 200, new MemorySizeConverter())
                .withMapping(BlindataProperties.class)
                .withMapping(OdmServicesProperties.class)
                .withSources(new PropertiesConfigSource(properties, "test", 500))
                .build();
    }
}
