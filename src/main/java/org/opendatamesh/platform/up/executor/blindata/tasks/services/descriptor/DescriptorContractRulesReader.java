package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.config.BlindataProperties;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRuleKind;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.SkippedRule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the contract rules of a descriptor, with the Blindata observer's mapping: every port whose datastore API
 * declares quality rules on its tables and columns; check code {@code <domain> - <name> - <rule id>}; physical binding
 * from {@code databaseSchemaName}, table and column; connection name from the port property
 * {@code x-blindataConnectionName}. The rule sent to the agent is the ODCS rule without its catalog-only keys, as TEST
 * RULES sends it. Rules the agent cannot run here are kept as skipped with the reason.
 */
@ApplicationScoped
public class DescriptorContractRulesReader {

    private static final List<String> PORT_GROUPS = List.of(
            "inputPorts", "outputPorts", "controlPorts", "discoveryPorts", "observabilityPorts");
    private static final Set<String> CATALOG_ONLY_KEYS = Set.of(
            "id", "name", "description", "dimension", "severity", "businessImpact", "tags",
            "authoritativeDefinitions", "scheduler", "schedule", "customProperties");
    private static final String DATASTORE_API = "datastoreapi";
    private static final String REF = "$ref";

    private final ObjectMapper json;
    private final String blockingProperty;
    private final String connectionProperty;

    public DescriptorContractRulesReader(BlindataProperties properties, ObjectMapper json) {
        this.json = json;
        this.blockingProperty = properties.contractRules().blockingProperty();
        this.connectionProperty = properties.contractRules().connectionProperty();
    }

    public ContractRules read(DataProductVersionDescriptor descriptor) {
        JsonNode root = descriptor.content();
        Collector collector = new Collector(suiteCode(root));
        for (String group : PORT_GROUPS) {
            for (JsonNode port : root.path("interfaceComponents").path(group)) {
                readPort(resolve(port, root), root, collector);
            }
        }
        return new ContractRules(collector.rules, collector.skipped);
    }

    private void readPort(JsonNode port, JsonNode root, Collector collector) {
        JsonNode api = resolve(port.path("promises").path("api"), root);
        if (!DATASTORE_API.equalsIgnoreCase(api.path("specification").asText())) {
            return;
        }
        JsonNode definition = parseIfText(resolve(api.path("definition"), root));
        JsonNode schema = resolve(definition.path("schema"), definition);
        PortContext context = new PortContext(text(port, "fullyQualifiedName"), connectionName(port),
                text(schema, "databaseSchemaName"));
        for (JsonNode table : schema.path("tables")) {
            readTable(resolve(table, definition), definition, context, collector);
        }
    }

    private void readTable(JsonNode table, JsonNode definitionRoot, PortContext port, Collector collector) {
        JsonNode definition = resolve(table.path("definition"), definitionRoot);
        String object = firstText(text(definition, "name"), text(table, "name"));
        for (JsonNode quality : definition.path("quality")) {
            readRule(resolve(quality, definitionRoot), port, object, null, collector);
        }
        Iterator<Map.Entry<String, JsonNode>> properties = definition.path("properties").fields();
        while (properties.hasNext()) {
            Map.Entry<String, JsonNode> entry = properties.next();
            JsonNode property = resolve(entry.getValue(), definitionRoot);
            String column = firstText(text(property, "name"), entry.getKey());
            for (JsonNode quality : property.path("quality")) {
                readRule(resolve(quality, definitionRoot), port, object, column, collector);
            }
        }
    }

    private void readRule(JsonNode quality, PortContext port, String object, String property, Collector collector) {
        if (isReference(quality) || isLegacy(quality)) {
            return;
        }
        String ruleId = text(quality, "id");
        String ruleName = displayName(quality);
        String type = ruleType(quality);
        String checkCode = ruleId == null ? null : collector.suiteCode + " - " + ruleId;
        ContractRule rule = new ContractRule(checkCode, ruleId, ruleName, port.fqn, port.schema, object, property,
                port.connectionName, type, isBlocking(quality), ContractRuleKind.RULE, executionRule(quality));
        String skipReason = skipReason(rule, collector);
        if (skipReason != null) {
            collector.skipped.add(SkippedRule.of(rule, skipReason));
            return;
        }
        collector.codes.add(checkCode);
        collector.rules.add(rule);
    }

    private static String skipReason(ContractRule rule, Collector collector) {
        if (rule.ruleId() == null) {
            return "the rule has no id (required by the descriptor convention, D11)";
        }
        if (rule.ruleType() == null) {
            return "the rule has no type and no metric or query to infer it";
        }
        if (!ContractRule.TYPE_LIBRARY.equals(rule.ruleType()) && !ContractRule.TYPE_SQL.equals(rule.ruleType())) {
            return "type " + rule.ruleType() + " is not executable";
        }
        if (rule.connectionName() == null) {
            return "the port declares no Blindata connection name";
        }
        if (collector.codes.contains(rule.checkCode())) {
            return "check code " + rule.checkCode() + " is declared more than once; the first declaration is kept";
        }
        return null;
    }

    private String connectionName(JsonNode port) {
        String name = text(port, connectionProperty);
        if (name == null && connectionProperty.startsWith("x-")) {
            name = text(port, connectionProperty.substring(2));
        }
        return name == null || name.isBlank() ? null : name;
    }

    /** Blocking unless {@code customProperties} sets the blocking key to false (object or ODCS property list form). */
    private boolean isBlocking(JsonNode quality) {
        JsonNode customProperties = quality.get("customProperties");
        if (customProperties == null) {
            return true;
        }
        if (customProperties.isObject()) {
            return !isFalse(customProperties.get(blockingProperty));
        }
        for (JsonNode entry : customProperties) {
            if (blockingProperty.equals(text(entry, "property"))) {
                return !isFalse(entry.get("value"));
            }
        }
        return true;
    }

    private static boolean isFalse(JsonNode value) {
        return value != null && (value.isBoolean() ? !value.asBoolean() : "false".equalsIgnoreCase(value.asText().trim()));
    }

    private static String ruleType(JsonNode quality) {
        String type = text(quality, "type");
        if (type != null) {
            return type.toLowerCase();
        }
        if (text(quality, "metric") != null) {
            return ContractRule.TYPE_LIBRARY;
        }
        return text(quality, "query") != null ? ContractRule.TYPE_SQL : null;
    }

    private ObjectNode executionRule(JsonNode quality) {
        ObjectNode rule = json.createObjectNode();
        quality.fields().forEachRemaining(field -> {
            if (!CATALOG_ONLY_KEYS.contains(field.getKey())) {
                rule.set(field.getKey(), field.getValue().deepCopy());
            }
        });
        String type = ruleType(quality);
        if (type != null && !rule.has("type")) {
            rule.put("type", type);
        }
        return rule;
    }

    private static String displayName(JsonNode quality) {
        return firstText(text(quality.path("customProperties"), "displayName"), text(quality, "name"), text(quality, "id"));
    }

    private static boolean isReference(JsonNode quality) {
        return text(quality.path("customProperties"), "refName") != null && text(quality, "type") == null;
    }

    private static boolean isLegacy(JsonNode quality) {
        return text(quality.path("customProperties"), "scoreStrategy") != null && text(quality, "type") == null;
    }

    private static String suiteCode(JsonNode root) {
        JsonNode info = root.path("info");
        return text(info, "domain") + " - " + text(info, "name");
    }

    private JsonNode parseIfText(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return node;
        }
        try {
            return json.readTree(node.asText());
        } catch (JsonProcessingException e) {
            return json.missingNode();
        }
    }

    /** Resolves an internal {@code $ref} ({@code #/a/b} or {@code #a.b}) against the given document root. */
    private static JsonNode resolve(JsonNode node, JsonNode root) {
        JsonNode current = node;
        for (int depth = 0; depth < 10 && current != null && current.isObject() && current.has(REF); depth++) {
            String ref = current.get(REF).asText();
            if (!ref.startsWith("#")) {
                return current;
            }
            String path = ref.substring(1);
            if (!path.startsWith("/")) {
                path = "/" + path.replace('.', '/');
            }
            JsonNode target = root.at(JsonPointer.compile(path));
            if (target.isMissingNode()) {
                return current;
            }
            current = target;
        }
        return current == null ? MissingNode.getInstance() : current;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isValueNode() && !value.isNull() && !value.asText().isBlank() ? value.asText() : null;
    }

    private record PortContext(String fqn, String connectionName, String schema) {
    }

    private static final class Collector {

        private final String suiteCode;
        private final List<ContractRule> rules = new ArrayList<>();
        private final List<SkippedRule> skipped = new ArrayList<>();
        private final Set<String> codes = new LinkedHashSet<>();

        private Collector(String suiteCode) {
            this.suiteCode = suiteCode;
        }
    }
}
