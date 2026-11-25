package org.opendatamesh.platform.up.executor.blindata.clients.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties;
import org.opendatamesh.platform.up.executor.blindata.config.OdmServicesProperties;
import org.opendatamesh.platform.up.executor.blindata.exceptions.BadGatewayException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientException;
import org.opendatamesh.platform.up.executor.blindata.exceptions.client.ClientResourceMappingException;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.utils.client.RestUtils;
import org.opendatamesh.platform.up.executor.blindata.utils.client.RestUtilsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RegistryClientImpl implements RegistryClient {

    private static final Logger logger = LoggerFactory.getLogger(RegistryClientImpl.class);

    private static final String PRODUCTS = "/api/v2/pp/registry/products";
    private static final String VERSIONS = "/api/v2/pp/registry/products-versions";
    private static final int HTTP_NOT_FOUND = 404;
    private static final int MAX_ERROR_CHARS = 300;

    private final String baseUrl;
    private final int readRetries;
    private final ObjectMapper objectMapper;
    private final RestUtils rest;

    public RegistryClientImpl(OdmServicesProperties odm, BlindataProperties blindata, ObjectMapper objectMapper) {
        this.baseUrl = trimTrailingSlash(odm.registryService().address().orElse(""));
        this.readRetries = blindata.agent().http().readRetries();
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(blindata.agent().http().connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.rest = RestUtilsFactory.getRestUtils(httpClient, objectMapper);
    }

    @Override
    public Optional<DataProductVersionDescriptor> findVersion(String dataProductFqn, String tag, String versionNumber) {
        Optional<String> dataProductUuid = findDataProductUuid(dataProductFqn);
        if (dataProductUuid.isEmpty()) {
            return Optional.empty();
        }
        return findVersionUuid(dataProductUuid.get(), tag, versionNumber)
                .flatMap(this::readVersion)
                .map(version -> toDescriptor(dataProductFqn, version));
    }

    private Optional<String> findDataProductUuid(String dataProductFqn) {
        JsonNode page = get(baseUrl + PRODUCTS + "?fqn=" + encode(dataProductFqn), "search the data product " + dataProductFqn)
                .orElse(null);
        for (JsonNode product : content(page)) {
            if (dataProductFqn.equals(text(product, "fqn"))) {
                return Optional.ofNullable(text(product, "uuid"));
            }
        }
        return Optional.empty();
    }

    private Optional<String> findVersionUuid(String dataProductUuid, String tag, String versionNumber) {
        boolean byNumber = versionNumber != null && !versionNumber.isBlank();
        String filter = byNumber ? "&versionNumber=" + encode(versionNumber) : "&tag=" + encode(tag);
        JsonNode page = get(baseUrl + VERSIONS + "?dataProductUuid=" + encode(dataProductUuid) + filter,
                "search the versions of data product " + dataProductUuid).orElse(null);
        for (JsonNode version : content(page)) {
            String candidate = byNumber ? text(version, "versionNumber") : text(version, "tag");
            if ((byNumber ? versionNumber : tag).equals(candidate)) {
                return Optional.ofNullable(text(version, "uuid"));
            }
        }
        return Optional.empty();
    }

    private Optional<JsonNode> readVersion(String versionUuid) {
        return get(baseUrl + VERSIONS + "/" + encode(versionUuid), "read the data product version " + versionUuid);
    }

    private DataProductVersionDescriptor toDescriptor(String dataProductFqn, JsonNode version) {
        return new DataProductVersionDescriptor(dataProductFqn, text(version, "versionNumber"), text(version, "tag"),
                descriptorContent(version.get("content")));
    }

    private JsonNode descriptorContent(JsonNode content) {
        if (content == null || !content.isTextual()) {
            return content;
        }
        try {
            return objectMapper.readTree(content.asText());
        } catch (JsonProcessingException e) {
            throw new BadGatewayException("ODM Registry returned a descriptor that is not valid JSON");
        }
    }

    private Optional<JsonNode> get(String url, String action) {
        int attempt = 0;
        while (true) {
            try {
                return Optional.ofNullable(rest.genericGet(url, null, null, JsonNode.class));
            } catch (ClientException e) {
                if (e.getCode() == HTTP_NOT_FOUND) {
                    return Optional.empty();
                }
                if (!isRetryable(e) || attempt >= readRetries) {
                    throw new BadGatewayException("ODM Registry failed to " + action + ": HTTP " + e.getCode()
                            + excerpt(e.getResponseBody()), e);
                }
                attempt++;
                logger.warn("ODM Registry call failed with HTTP {}, retry {} of {}", e.getCode(), attempt, readRetries);
            } catch (ClientResourceMappingException e) {
                throw new BadGatewayException("ODM Registry returned an unreadable response when trying to " + action, e);
            }
        }
    }

    private static boolean isRetryable(ClientException e) {
        return e.getCode() >= 500 || e.getCode() == 429;
    }

    private static Iterable<JsonNode> content(JsonNode page) {
        if (page == null) {
            return List.of();
        }
        JsonNode content = page.isArray() ? page : page.get("content");
        return content != null && content.isArray() ? content : List.of();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
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
