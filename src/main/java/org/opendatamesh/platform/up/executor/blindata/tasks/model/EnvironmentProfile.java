package org.opendatamesh.platform.up.executor.blindata.tasks.model;

/**
 * Everything the executor needs to verify a contract in one environment: the agent, the Blindata tenant of that
 * environment, its credentials, how connection names resolve, and the target platform.
 */
public record EnvironmentProfile(String name, String agentUrl, String tenantUuid, String connectionNameTemplate,
                                 Platform platform, BlindataCredentials credentials) {

    public static final String NAME_PLACEHOLDER = "{name}";
    public static final String ENVIRONMENT_PLACEHOLDER = "{environment}";

    public String agentConnectionName(String descriptorConnectionName) {
        return connectionNameTemplate
                .replace(NAME_PLACEHOLDER, descriptorConnectionName)
                .replace(ENVIRONMENT_PLACEHOLDER, name);
    }
}
