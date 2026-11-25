package org.opendatamesh.platform.up.executor.blindata.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties.Auth;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties.Environment;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.BlindataCredentials;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

/** Turns the configured environment profiles into domain profiles. The map was validated at startup. */
@ApplicationScoped
public class EnvironmentProfiles {

    private final BlindataProperties properties;

    public EnvironmentProfiles(BlindataProperties properties) {
        this.properties = properties;
    }

    public Optional<EnvironmentProfile> find(String environment) {
        if (environment == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(properties.environments().get(environment))
                .map(configured -> toProfile(environment, configured));
    }

    private static EnvironmentProfile toProfile(String name, Environment environment) {
        return new EnvironmentProfile(name, trimTrailingSlash(environment.agentUrl().orElseThrow()),
                environment.tenantUuid().orElseThrow(), environment.connectionNameTemplate(), environment.platform(),
                credentials(environment.auth()));
    }

    private static BlindataCredentials credentials(Auth auth) {
        return switch (auth.type().orElseThrow()) {
            case API_KEY -> new BlindataCredentials.ApiKey(auth.apiKey().user().orElseThrow(), auth.apiKey().key().orElseThrow());
            case OAUTH2_CLIENT_CREDENTIALS -> new BlindataCredentials.OAuth2ClientCredentials(
                    auth.oauth2().url().orElseThrow(), auth.oauth2().grantType(), auth.oauth2().scope().orElseThrow(),
                    auth.oauth2().clientId().orElseThrow(), auth.oauth2().clientSecret().orElse(null),
                    auth.oauth2().clientCertificate().orElse(null), auth.oauth2().clientCertificatePrivateKey().orElse(null));
        };
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
