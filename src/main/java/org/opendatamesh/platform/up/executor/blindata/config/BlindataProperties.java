package org.opendatamesh.platform.up.executor.blindata.config;

import io.quarkus.runtime.configuration.MemorySize;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.Platform;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * The Blindata side: the agent client, how contract rules are read, and the environment profiles. The profiles are a
 * deployment-defined map: they are never packaged and come from the mounted profiles file.
 */
@ConfigMapping(prefix = "blindata")
public interface BlindataProperties {

    Agent agent();

    ContractRules contractRules();

    /** Key: the {@code environment} pipeline parameter. */
    Map<String, Environment> environments();

    interface Agent {

        Http http();

        Logs logs();

        interface Http {

            @WithDefault("5s")
            Duration connectTimeout();

            /** Retries of reads (job history and logs, Registry); never of the job start. */
            @WithDefault("2")
            int readRetries();
        }

        interface Logs {

            /** Upper bound of the agent job log copied into the task logs. */
            @WithDefault("1M")
            MemorySize maxAgentLogSize();
        }
    }

    interface ContractRules {

        /** ODCS {@code customProperties} key that marks a rule as non-blocking with value {@code false} (D11). */
        @WithDefault("blocking")
        String blockingProperty();

        /** Port property holding the Blindata connection name; same key as the observer's. */
        @WithDefault("x-blindataConnectionName")
        String connectionProperty();
    }

    interface Environment {

        Optional<String> agentUrl();

        Optional<String> tenantUuid();

        /** {@code {name}} is the descriptor connection name, {@code {environment}} the environment key. */
        @WithDefault("{name}")
        String connectionNameTemplate();

        @WithDefault("GENERIC")
        Platform platform();

        Auth auth();
    }

    interface Auth {

        Optional<AuthType> type();

        ApiKey apiKey();

        OAuth2 oauth2();

        enum AuthType {
            API_KEY,
            OAUTH2_CLIENT_CREDENTIALS
        }

        /** Observer keys: {@code blindata.user} and {@code blindata.password}. */
        interface ApiKey {

            Optional<String> user();

            Optional<String> key();
        }

        /** Observer keys: {@code blindata.oauth2.*}. */
        interface OAuth2 {

            Optional<String> url();

            @WithDefault("client_credentials")
            String grantType();

            Optional<String> scope();

            Optional<String> clientId();

            Optional<String> clientSecret();

            Optional<String> clientCertificate();

            Optional<String> clientCertificatePrivateKey();
        }
    }
}
