package org.opendatamesh.platform.up.executor.blindata.config;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.testsupport.TestConfigs;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BDMD-5755 suggested configuration: startup validation names the offending key. */
class ExecutorBlindataConfigValidatorTest {

    @Test
    void whenTheConfigurationIsValidThenStartupPasses() {
        assertDoesNotThrow(() -> validator(TestConfigs.valid()).validate());
    }

    @Test
    void whenAProfileIsIncompleteThenStartupFailsNamingTheKey() {
        assertFails(p -> p.remove("blindata.environments.dev.tenant-uuid"), "blindata.environments.dev.tenant-uuid");
        assertFails(p -> p.remove("blindata.environments.dev.auth.api-key.key"), "blindata.environments.dev.auth.api-key.key");
        assertFails(p -> p.remove("blindata.environments.prod.auth.oauth2.client-secret"), "blindata.environments.prod.auth.oauth2");
        assertFails(p -> p.remove("blindata.environments.prod.auth.type"), "blindata.environments.prod.auth.type");
        assertFails(p -> p.put("blindata.environments.dev.agent-url", "agent:8082"), "blindata.environments.dev.agent-url");
        assertFails(p -> p.put("blindata.environments.dev.connection-name-template", "fixed"), "connection-name-template");
    }

    @Test
    void whenACertificateReplacesTheSecretThenStartupPasses() {
        Map<String, String> properties = TestConfigs.valid();
        properties.remove("blindata.environments.prod.auth.oauth2.client-secret");
        properties.put("blindata.environments.prod.auth.oauth2.client-certificate", "-----BEGIN CERTIFICATE-----");
        properties.put("blindata.environments.prod.auth.oauth2.client-certificate-private-key", "-----BEGIN PRIVATE KEY-----");

        assertDoesNotThrow(() -> validator(properties).validate());
    }

    @Test
    void whenTheRegistryOrTheEnvironmentsAreMissingThenStartupFails() {
        assertFails(p -> p.remove("odm.product-plane.registry-service.address"), "odm.product-plane.registry-service.address");
        assertFails(p -> p.put("odm.product-plane.devops-service.address", ""), "odm.product-plane.devops-service.address");
        assertFails(p -> p.keySet().removeIf(k -> k.startsWith("blindata.environments")), "blindata.environments");
    }

    private static void assertFails(Consumer<Map<String, String>> change, String expectedKey) {
        Map<String, String> properties = TestConfigs.valid();
        change.accept(properties);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> validator(properties).validate());
        assertTrue(e.getMessage().contains(expectedKey), e.getMessage());
    }

    private static ExecutorBlindataConfigValidator validator(Map<String, String> properties) {
        return new ExecutorBlindataConfigValidator(TestConfigs.odm(properties), TestConfigs.blindata(properties));
    }
}
