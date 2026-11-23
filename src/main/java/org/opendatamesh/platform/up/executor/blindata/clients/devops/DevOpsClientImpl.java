package org.opendatamesh.platform.up.executor.blindata.clients.devops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties;
import org.opendatamesh.platform.up.executor.blindata.config.OdmServicesProperties;
import org.opendatamesh.platform.up.executor.blindata.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.InternalException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientResourceMappingException;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;
import org.opendatamesh.platform.up.executor.blindata.utils.client.RestUtils;
import org.opendatamesh.platform.up.executor.blindata.utils.client.RestUtilsFactory;

import java.net.http.HttpClient;
import java.time.Instant;

@ApplicationScoped
public class DevOpsClientImpl implements DevOpsClient {

    private static final String TASK_RESULTS = "/api/v2/pp/devops/activities/tasks/results";
    private static final int MAX_ERROR_CHARS = 300;

    private final boolean active;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final VerificationReportJson reportJson;
    private final RestUtils rest;

    public DevOpsClientImpl(OdmServicesProperties odm, BlindataProperties blindata, ObjectMapper objectMapper) {
        this.active = odm.devopsService().active();
        this.baseUrl = odm.devopsService().address().map(DevOpsClientImpl::trimTrailingSlash).orElse("");
        this.objectMapper = objectMapper;
        this.reportJson = new VerificationReportJson(objectMapper);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(blindata.agent().http().connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.rest = RestUtilsFactory.getRestUtils(httpClient, objectMapper);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void postResults(VerificationReport report) {
        try {
            rest.genericPost(baseUrl + TASK_RESULTS, null, command(report), JsonNode.class);
        } catch (ClientException e) {
            throw new BadGatewayException("ODM DevOps refused the results of task " + report.context().taskName()
                    + " of activity " + report.context().activityName() + ": HTTP " + e.getCode() + excerpt(e.getResponseBody()), e);
        } catch (ClientResourceMappingException e) {
            throw new BadGatewayException("ODM DevOps returned an unreadable response to the results post", e);
        }
    }

    private ObjectNode command(VerificationReport report) {
        ObjectNode command = objectMapper.createObjectNode();
        command.put("dataProductFqn", report.context().dataProductFqn());
        command.put("dataProductVersionTag", report.context().versionTag());
        command.put("activityName", report.context().activityName());
        command.put("taskName", report.context().taskName());
        ObjectNode result = command.putArray("results").addObject();
        result.put("content", serialize(reportJson.toJson(report)));
        result.put("generatedAt", Instant.now().toString());
        return command;
    }

    private String serialize(ObjectNode report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new InternalException("Cannot serialize the verification report", e);
        }
    }

    private static String excerpt(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String trimmed = body.strip();
        return ": " + (trimmed.length() > MAX_ERROR_CHARS ? trimmed.substring(0, MAX_ERROR_CHARS) + "…" : trimmed);
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
