package org.opendatamesh.platform.up.executor.blindata.clients.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties;
import org.opendatamesh.platform.up.executor.blindata.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientFailures;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientTransportException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientResourceMappingException;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.BlindataCredentials;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractJob;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;
import org.opendatamesh.platform.up.executor.blindata.utils.client.RestUtils;
import org.opendatamesh.platform.up.executor.blindata.utils.client.RestUtilsFactory;
import org.opendatamesh.platform.up.executor.blindata.utils.client.http.HttpHeader;
import org.opendatamesh.platform.up.executor.blindata.utils.client.http.Oauth2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Talks to the Blindata agent of an environment profile, with that profile's tenant and credentials. API keys go in
 * {@code X-BD-User} / {@code X-BD-ApiKey}; OAuth2 client credentials (secret or certificate) are exchanged for a bearer
 * token by the template's authenticated client, the same mechanism as the Blindata observer. Credentials and tokens are
 * never logged nor put in error messages.
 */
@ApplicationScoped
public class AgentClientImpl implements AgentClient {

    private static final Logger logger = LoggerFactory.getLogger(AgentClientImpl.class);

    private static final String RUN_ASYNC = "/agent-api/v1/jobs/runner/async";
    private static final String HISTORY = "/agent-api/v1/jobs/history/";
    private static final String TENANT_HEADER = "X-BD-Tenant";
    private static final String USER_HEADER = "X-BD-User";
    private static final String API_KEY_HEADER = "X-BD-ApiKey";
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int MAX_ERROR_CHARS = 300;
    private static final String TRUNCATED = "\n… agent log truncated …\n";

    private final BlindataProperties properties;
    private final ObjectMapper objectMapper;
    private final AgentJobMapper mapper;
    private final HttpClient httpClient;
    private final Map<String, RestUtils> clientsByEnvironment = new ConcurrentHashMap<>();

    public AgentClientImpl(BlindataProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.mapper = new AgentJobMapper(objectMapper);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.agent().http().connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public long launch(EnvironmentProfile profile, ContractJob job) {
        logger.debug("Launching job on the Blindata agent of environment {}: {} rules", profile.name(), job.rules().size());
        try {
            JsonNode history = rest(profile).genericPost(profile.agentUrl() + RUN_ASYNC, null, mapper.toJob(job), JsonNode.class);
            JsonNode sequenceId = history == null ? null : history.get("sequenceId");
            if (sequenceId == null || !sequenceId.isIntegralNumber()) {
                throw new BadGatewayException("Blindata agent of environment " + profile.name() + " did not return a job sequence id");
            }
            return sequenceId.asLong();
        } catch (ClientException e) {
            throw translate(profile, "start the verification job", e);
        } catch (ClientResourceMappingException e) {
            throw new BadGatewayException("Blindata agent of environment " + profile.name() + " returned an unreadable response to the job start", e);
        } catch (IllegalStateException e) {
            throw new BadGatewayException("Cannot authenticate to the Blindata agent of environment " + profile.name() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<AgentRun> readRun(EnvironmentProfile profile, long sequenceId) {
        return read(profile, HISTORY + sequenceId, "read job " + sequenceId).map(mapper::fromHistory);
    }

    @Override
    public Optional<String> readLog(EnvironmentProfile profile, long sequenceId) {
        return read(profile, HISTORY + sequenceId + "/logs", "read the log of job " + sequenceId)
                .map(node -> node.path("logContent").asText(""))
                .map(AgentClientImpl::normalizeLineBreaks)
                .map(this::bound);
    }

    private Optional<JsonNode> read(EnvironmentProfile profile, String path, String action) {
        int retries = properties.agent().http().readRetries();
        int attempt = 0;
        while (true) {
            try {
                logger.debug("Blindata agent of environment {}: {}", profile.name(), action);
                return Optional.ofNullable(rest(profile).genericGet(profile.agentUrl() + path, null, null, JsonNode.class));
            } catch (ClientException e) {
                if (e.getCode() == HTTP_NOT_FOUND) {
                    logger.debug("Blindata agent of environment {} answered 404 when trying to {}", profile.name(), action);
                    return Optional.empty();
                }
                if (!isRetryable(e) || attempt >= retries) {
                    throw translate(profile, action, e);
                }
                attempt++;
                logger.warn("Blindata agent of environment {} failed to {} with HTTP {}, retry {} of {}",
                        profile.name(), action, e.getCode(), attempt, retries);
            } catch (ClientResourceMappingException e) {
                throw new BadGatewayException("Blindata agent of environment " + profile.name() + " returned an unreadable response when trying to " + action, e);
            } catch (IllegalStateException e) {
                throw new BadGatewayException("Cannot authenticate to the Blindata agent of environment " + profile.name() + ": " + e.getMessage(), e);
            }
        }
    }

    private RestUtils rest(EnvironmentProfile profile) {
        return clientsByEnvironment.computeIfAbsent(profile.name(), name -> authenticatedRest(profile));
    }

    private RestUtils authenticatedRest(EnvironmentProfile profile) {
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader(TENANT_HEADER, profile.tenantUuid()));
        Oauth2 oauth2 = null;
        if (profile.credentials() instanceof BlindataCredentials.ApiKey apiKey) {
            headers.add(new HttpHeader(USER_HEADER, apiKey.user()));
            headers.add(new HttpHeader(API_KEY_HEADER, apiKey.key()));
        } else if (profile.credentials() instanceof BlindataCredentials.OAuth2ClientCredentials client) {
            oauth2 = oauth2(client);
        }
        return RestUtilsFactory.getAuthenticatedRestUtils(httpClient, objectMapper, headers, oauth2);
    }

    private static Oauth2 oauth2(BlindataCredentials.OAuth2ClientCredentials client) {
        Oauth2 oauth2 = new Oauth2();
        oauth2.setUrl(client.url());
        oauth2.setGrantType(client.grantType());
        oauth2.setScope(client.scope());
        oauth2.setClientId(client.clientId());
        oauth2.setClientSecret(client.clientSecret());
        oauth2.setClientCertificate(client.clientCertificate());
        oauth2.setClientCertificatePrivateKey(client.clientCertificatePrivateKey());
        return oauth2;
    }

    private static BadGatewayException translate(EnvironmentProfile profile, String action, ClientException e) {
        String agent = "Blindata agent of environment " + profile.name();
        if (e.getCode() == HTTP_UNAUTHORIZED || e.getCode() == HTTP_FORBIDDEN) {
            return new BadGatewayException(agent + " refused the credentials of the environment profile (HTTP " + e.getCode()
                    + ") when trying to " + action, e);
        }
        String sentence = agent + " failed to " + action + ClientFailures.describe(e, MAX_ERROR_CHARS);
        if (e instanceof ClientTransportException transport && transport.isResponseTimeout() && action.startsWith("start")) {
            sentence += ". The job may have started on the agent anyway";
        }
        return new BadGatewayException(sentence, e);
    }

    private static boolean isRetryable(ClientException e) {
        return e.getCode() >= 500 || e.getCode() == 429;
    }

    private String bound(String log) {
        long max = properties.agent().logs().maxAgentLogSize().asLongValue();
        if (log.length() <= max) {
            return log;
        }
        int half = (int) (max / 2);
        return log.substring(0, half) + TRUNCATED + log.substring(log.length() - half);
    }

    /** The agent separates log lines with a bare carriage return. */
    private static String normalizeLineBreaks(String log) {
        return log.replace("\r\n", "\n").replace('\r', '\n');
    }

}
