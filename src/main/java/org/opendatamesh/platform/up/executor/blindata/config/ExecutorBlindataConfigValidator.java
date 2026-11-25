package org.opendatamesh.platform.up.executor.blindata.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties.Auth;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties.Environment;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Stops the service at boot on invalid configuration, naming the offending key. */
@ApplicationScoped
public class ExecutorBlindataConfigValidator {

    private static final String REGISTRY_KEY = "odm.product-plane.registry-service";
    private static final String DEVOPS_KEY = "odm.product-plane.devops-service";
    private static final String ENVIRONMENTS_KEY = "blindata.environments";
    private static final Pattern ENVIRONMENT_NAME = Pattern.compile("[A-Za-z0-9_-]+");

    private final OdmServicesProperties odm;
    private final BlindataProperties blindata;

    public ExecutorBlindataConfigValidator(OdmServicesProperties odm, BlindataProperties blindata) {
        this.odm = odm;
        this.blindata = blindata;
    }

    void onStart(@Observes StartupEvent event) {
        validate();
    }

    public void validate() {
        validateRegistry();
        validateDevOps();
        validateAgent();
        validateContractRules();
        validateEnvironments(blindata.environments());
    }

    private void validateRegistry() {
        if (!odm.registryService().active()) {
            throw new IllegalStateException(REGISTRY_KEY + ".active: the Registry is required and must be active");
        }
        requireHttpUrl(odm.registryService().address(), REGISTRY_KEY + ".address");
    }

    private void validateDevOps() {
        if (odm.devopsService().active()) {
            requireHttpUrl(odm.devopsService().address(), DEVOPS_KEY + ".address");
        }
    }

    private void validateAgent() {
        Duration connectTimeout = blindata.agent().http().connectTimeout();
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalStateException("blindata.agent.http.connect-timeout: must be positive");
        }
        if (blindata.agent().http().readRetries() < 0) {
            throw new IllegalStateException("blindata.agent.http.read-retries: must be >= 0");
        }
        if (blindata.agent().logs().maxAgentLogSize().asLongValue() <= 0) {
            throw new IllegalStateException("blindata.agent.logs.max-agent-log-size: must be positive");
        }
    }

    private void validateContractRules() {
        requireText(Optional.ofNullable(blindata.contractRules().blockingProperty()), "blindata.contract-rules.blocking-property");
        requireText(Optional.ofNullable(blindata.contractRules().connectionProperty()), "blindata.contract-rules.connection-property");
    }

    private void validateEnvironments(Map<String, Environment> environments) {
        if (environments == null || environments.isEmpty()) {
            throw new IllegalStateException(ENVIRONMENTS_KEY + ": at least one environment profile is required");
        }
        environments.forEach(this::validateEnvironment);
    }

    private void validateEnvironment(String name, Environment environment) {
        String key = ENVIRONMENTS_KEY + "." + name;
        if (!ENVIRONMENT_NAME.matcher(name).matches()) {
            throw new IllegalStateException(key + ": the environment name must match [A-Za-z0-9_-]+");
        }
        requireHttpUrl(environment.agentUrl(), key + ".agent-url");
        requireText(environment.tenantUuid(), key + ".tenant-uuid");
        if (!environment.connectionNameTemplate().contains("{name}")) {
            throw new IllegalStateException(key + ".connection-name-template: must contain {name}");
        }
        validateAuth(environment.auth(), key + ".auth");
    }

    private void validateAuth(Auth auth, String key) {
        Auth.AuthType type = auth.type().orElseThrow(() ->
                new IllegalStateException(key + ".type: required (API_KEY or OAUTH2_CLIENT_CREDENTIALS)"));
        switch (type) {
            case API_KEY -> {
                requireText(auth.apiKey().user(), key + ".api-key.user");
                requireText(auth.apiKey().key(), key + ".api-key.key");
            }
            case OAUTH2_CLIENT_CREDENTIALS -> validateOAuth2(auth.oauth2(), key + ".oauth2");
        }
    }

    private void validateOAuth2(Auth.OAuth2 oauth2, String key) {
        requireHttpUrl(oauth2.url(), key + ".url");
        requireText(Optional.ofNullable(oauth2.grantType()), key + ".grant-type");
        requireText(oauth2.scope(), key + ".scope");
        requireText(oauth2.clientId(), key + ".client-id");
        boolean secret = hasText(oauth2.clientSecret());
        boolean certificate = hasText(oauth2.clientCertificate()) && hasText(oauth2.clientCertificatePrivateKey());
        if (!secret && !certificate) {
            throw new IllegalStateException(key + ": client-secret, or client-certificate and client-certificate-private-key, are required");
        }
    }

    private static void requireHttpUrl(Optional<String> value, String key) {
        requireText(value, key);
        try {
            URI uri = URI.create(value.get().trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalStateException(key + ": not an absolute http or https URL with a host");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(key + ": malformed URL");
        }
    }

    private static void requireText(Optional<String> value, String key) {
        if (!hasText(value)) {
            throw new IllegalStateException(key + ": required");
        }
    }

    private static boolean hasText(Optional<String> value) {
        return value != null && value.isPresent() && !value.get().isBlank();
    }
}
