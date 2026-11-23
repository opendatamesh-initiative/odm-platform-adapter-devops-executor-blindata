package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRuleKind;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.SkippedRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.OdcsQualityRule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns the ODCS rules met by the visitors into contract rules, with the observer's mapping: check code
 * {@code <domain> - <name> - <rule id>}, the ODCS rule without its catalog-only keys as the rule the agent runs (TEST
 * RULES parity), blocking unless {@code customProperties} set the blocking key to {@code false}. Rules that cannot run
 * are kept as skipped, with the reason.
 */
final class ContractRulesCollector {

    private static final Set<String> CATALOG_ONLY_KEYS = Set.of(
            "id", "name", "description", "dimension", "severity", "businessImpact", "tags",
            "authoritativeDefinitions", "scheduler", "schedule", "customProperties");
    private static final Set<String> COMPONENT_BASE_KEYS = Set.of("$ref", "mediaType", "baseUri");

    private final ObjectMapper json;
    private final String blockingProperty;
    private final List<ContractRule> rules = new ArrayList<>();
    private final List<SkippedRule> skipped = new ArrayList<>();
    private final Set<String> codes = new LinkedHashSet<>();
    private String suiteCode;

    ContractRulesCollector(ObjectMapper json, String blockingProperty) {
        this.json = json;
        this.blockingProperty = blockingProperty;
    }

    void dataProduct(String domain, String name) {
        this.suiteCode = domain + " - " + name;
    }

    void rule(OdcsQualityRule quality, RuleLocation location) {
        if (isReference(quality) || isLegacy(quality)) {
            return;
        }
        String ruleId = blankToNull(quality.getId());
        ContractRule rule = new ContractRule(ruleId == null ? null : suiteCode + " - " + ruleId, ruleId,
                displayName(quality), location.portFqn(), location.schema(), location.object(), location.property(),
                location.connectionName(), ruleType(quality), isBlocking(quality), ContractRuleKind.RULE,
                executionRule(quality));
        String skipReason = skipReason(rule);
        if (skipReason != null) {
            skipped.add(SkippedRule.of(rule, skipReason));
            return;
        }
        codes.add(rule.checkCode());
        rules.add(rule);
    }

    /** A table whose definition the ODCS plugin did not read (another specification) but that declares quality rules. */
    void unreadTable(RuleLocation location, String reason) {
        skipped.add(new SkippedRule(null, null, null, location.portFqn(), location.object(), null, true, reason));
    }

    ContractRules result() {
        return new ContractRules(rules, skipped);
    }

    private String skipReason(ContractRule rule) {
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
        if (codes.contains(rule.checkCode())) {
            return "check code " + rule.checkCode() + " is declared more than once; the first declaration is kept";
        }
        return null;
    }

    private boolean isBlocking(OdcsQualityRule quality) {
        JsonNode customProperties = quality.getCustomProperties();
        if (customProperties == null || customProperties.isNull()) {
            return true;
        }
        if (customProperties.isObject()) {
            return !isFalse(customProperties.get(blockingProperty));
        }
        for (JsonNode entry : customProperties) {
            if (blockingProperty.equals(entry.path("property").asText(null))) {
                return !isFalse(entry.get("value"));
            }
        }
        return true;
    }

    private ObjectNode executionRule(OdcsQualityRule quality) {
        ObjectNode rule = json.valueToTree(quality);
        Iterator<java.util.Map.Entry<String, JsonNode>> fields = rule.fields();
        while (fields.hasNext()) {
            java.util.Map.Entry<String, JsonNode> field = fields.next();
            if (CATALOG_ONLY_KEYS.contains(field.getKey()) || COMPONENT_BASE_KEYS.contains(field.getKey())
                    || field.getValue() == null || field.getValue().isNull()) {
                fields.remove();
            }
        }
        String type = ruleType(quality);
        if (type != null && !rule.has("type")) {
            rule.put("type", type);
        }
        return rule;
    }

    /** As the agent infers it: {@code sql} when the rule has a query, {@code library} when it has a metric. */
    private static String ruleType(OdcsQualityRule quality) {
        if (blankToNull(quality.getType()) != null) {
            return quality.getType().toLowerCase();
        }
        if (blankToNull(quality.getQuery()) != null) {
            return ContractRule.TYPE_SQL;
        }
        return blankToNull(quality.getMetric()) != null || blankToNull(quality.getRule()) != null
                ? ContractRule.TYPE_LIBRARY : null;
    }

    private static String displayName(OdcsQualityRule quality) {
        JsonNode customProperties = quality.getCustomProperties();
        String displayName = customProperties != null && customProperties.isObject()
                ? blankToNull(customProperties.path("displayName").asText(null)) : null;
        if (displayName != null) {
            return displayName;
        }
        return blankToNull(quality.getName()) != null ? quality.getName() : quality.getId();
    }

    private static boolean isReference(OdcsQualityRule quality) {
        return hasCustomProperty(quality, "refName") && blankToNull(quality.getType()) == null;
    }

    private static boolean isLegacy(OdcsQualityRule quality) {
        return hasCustomProperty(quality, "scoreStrategy") && blankToNull(quality.getType()) == null;
    }

    private static boolean hasCustomProperty(OdcsQualityRule quality, String key) {
        JsonNode customProperties = quality.getCustomProperties();
        return customProperties != null && customProperties.isObject()
                && blankToNull(customProperties.path(key).asText(null)) != null;
    }

    private static boolean isFalse(JsonNode value) {
        return value != null && (value.isBoolean() ? !value.asBoolean() : "false".equalsIgnoreCase(value.asText().trim()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
