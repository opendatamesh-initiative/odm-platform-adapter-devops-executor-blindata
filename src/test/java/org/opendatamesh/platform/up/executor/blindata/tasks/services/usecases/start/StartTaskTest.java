package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.junit.jupiter.api.Test;
import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentRuleSupport;
import org.opendatamesh.platform.up.executor.blindata.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.BlindataCredentials;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractJob;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.Platform;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.DescriptorContractRulesReader;
import org.opendatamesh.platform.up.executor.blindata.testsupport.Fixtures;
import org.opendatamesh.platform.up.executor.blindata.testsupport.TestConfigs;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BDMD-5755 decisions 2, 4 and 13: start of a contract verification, with fake ports. */
class StartTaskTest {

    private static final EnvironmentProfile DEV = new EnvironmentProfile("dev", "http://agent", "tenant-dev",
            "{name}-{environment}", Platform.BIGQUERY, new BlindataCredentials.ApiKey("u", "k"));

    private final DescriptorContractRulesReader reader =
            new DescriptorContractRulesReader(TestConfigs.blindata(TestConfigs.valid()));
    private ContractJob launched;
    private ProviderRunId presented;

    /**
     * Scenario: start launches one dry-run job and returns <environment>:<sequenceId>
     *   Given environment "dev" (BigQuery, connection template "{name}-{environment}") and version tag "v1.2.0"
     *   When the task starts
     *   Then the agent receives the executable rules with connection "bq-sales-dev"
     *   And the non-blocking pattern rule is skipped because BigQuery cannot run it
     *   And the provider run id is "dev:42"
     */
    @Test
    void whenStartingThenTheJobIsLaunchedWithResolvedConnectionsAndSupportedRules() {
        start(command("dev", "v1.2.0"), Fixtures.ordersDescriptor());

        assertEquals("dev:42", presented.value());
        assertEquals(5, launched.rules().size());
        assertTrue(launched.rules().stream().allMatch(r -> r.connectionName().equals("bq-sales-dev")));
        assertTrue(launched.skipped().stream().anyMatch(s -> "country_code_pattern".equals(s.ruleId())
                && s.reason().contains("REGEXP_CONTAINS")));
        assertEquals("deploy-dev", launched.context().activityName());
        assertEquals("1.2.0", launched.context().dataProductVersion());
    }

    /** Scenario: a blocking rule the agent cannot run on the platform rejects the start (400), nothing is launched. */
    @Test
    void whenABlockingRuleCannotRunThenTheStartIsRejected() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> start(command("dev", "v1.3.0"), Fixtures.ordersDescriptorWithBlockingPattern()));

        assertTrue(e.getMessage().contains("country_code_pattern"));
        assertTrue(e.getMessage().contains("REGEXP_CONTAINS"));
        assertNull(launched);
    }

    /** Scenario: missing parameters, unknown environment or unknown version → 400. */
    @Test
    void whenTheRequestCannotBeResolvedThenBadRequest() {
        assertTrue(assertThrows(BadRequestException.class, () -> start(
                new StartTaskCommand(null, "dev", "v1", null, null, null), Fixtures.ordersDescriptor()))
                .getMessage().contains("dataProductFqn"));
        assertTrue(assertThrows(BadRequestException.class, () -> start(
                new StartTaskCommand(Fixtures.ORDERS_FQN, "dev", null, null, null, null), Fixtures.ordersDescriptor()))
                .getMessage().contains("ref.name"));
        assertTrue(assertThrows(BadRequestException.class, () -> start(command("staging", "v1.2.0"), Fixtures.ordersDescriptor()))
                .getMessage().contains("Unknown environment staging"));
        assertTrue(assertThrows(BadRequestException.class, () -> start(command("dev", "v9"), null))
                .getMessage().contains("not found in the Registry"));
    }

    private void start(StartTaskCommand command, JsonNode descriptor) {
        new StartTask(command, runId -> presented = runId,
                environment -> "dev".equals(environment) ? Optional.of(DEV) : Optional.empty(),
                (fqn, tag, number) -> descriptor == null ? Optional.empty()
                        : Optional.of(new DataProductVersionDescriptor(fqn, descriptor.at("/info/version").asText(), tag, descriptor)),
                new StartTaskRulesOutboundPort() {
                    @Override
                    public org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules readRules(DataProductVersionDescriptor d) {
                        return reader.read(d);
                    }

                    @Override
                    public Optional<String> unsupportedReason(ContractRule rule, Platform platform) {
                        return AgentRuleSupport.unsupportedReason(rule, platform);
                    }
                },
                (profile, job) -> {
                    launched = job;
                    return 42L;
                }).execute();
    }

    private static StartTaskCommand command(String environment, String tag) {
        return new StartTaskCommand(Fixtures.ORDERS_FQN, environment, tag, null, "deploy-dev", "verify-contract");
    }
}
