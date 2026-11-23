package org.opendatamesh.platform.up.executor.blindata.clients.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What the agent's {@code CONTRACT_RULE} probe can run, by platform. The agent builds library SQL with no dialect switch
 * and accepts only plain identifiers ({@code ContractRuleJdbcLibrary}); a rule outside these limits would come back as an
 * error row on every run.
 */
public final class AgentRuleSupport {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z0-9_]+");
    private static final Set<String> LIBRARY_METRICS = Set.of(
            "nullValues", "missingValues", "invalidValues", "duplicateValues", "rowCount");

    private AgentRuleSupport() {
    }

    /** Why the agent cannot run this rule on the platform, or empty when it can. */
    public static Optional<String> unsupportedReason(ContractRule rule, Platform platform) {
        List<String> reasons = new ArrayList<>();
        bindingReasons(rule, reasons);
        if (ContractRule.TYPE_LIBRARY.equalsIgnoreCase(rule.ruleType())) {
            libraryReasons(rule, platform, reasons);
        }
        return reasons.isEmpty() ? Optional.empty() : Optional.of(String.join("; ", reasons));
    }

    private static void bindingReasons(ContractRule rule, List<String> reasons) {
        if (needsObject(rule) && (isBlank(rule.schema()) || isBlank(rule.object()))) {
            reasons.add("the physical binding needs a schema and an object (datastore API databaseSchemaName and table name)");
        }
        identifier(rule.schema(), "schema", reasons);
        identifier(rule.object(), "object", reasons);
        identifier(rule.property(), "property", reasons);
    }

    private static void libraryReasons(ContractRule rule, Platform platform, List<String> reasons) {
        String metric = rule.metric();
        if (metric == null || !LIBRARY_METRICS.contains(metric)) {
            reasons.add("library metric \"" + metric + "\" is not supported by the agent (supported: " + String.join(", ", LIBRARY_METRICS) + ")");
            return;
        }
        JsonNode arguments = rule.executionRule().get("arguments");
        if (platform == Platform.BIGQUERY && "invalidValues".equals(metric) && hasPattern(arguments)) {
            reasons.add("invalidValues with a pattern is not supported by the agent on BigQuery (it emits REGEXP_LIKE and CAST AS VARCHAR); "
                    + "write it as a sql rule, e.g. SELECT COUNT(*) FROM {object} WHERE {property} IS NULL OR NOT REGEXP_CONTAINS(CAST({property} AS STRING), r'"
                    + arguments.get("pattern").asText() + "')");
        }
        if ("duplicateValues".equals(metric) && arguments != null && arguments.has("properties") && arguments.get("properties").isArray()) {
            for (JsonNode property : arguments.get("properties")) {
                identifier(property.asText(), "arguments.properties", reasons);
            }
        }
    }

    private static void identifier(String value, String what, List<String> reasons) {
        if (!isBlank(value) && !IDENTIFIER.matcher(value).matches()) {
            reasons.add(what + " \"" + value + "\" is not a plain identifier [a-zA-Z0-9_]+ (the agent emits identifiers unquoted)");
        }
    }

    private static boolean needsObject(ContractRule rule) {
        if (ContractRule.TYPE_LIBRARY.equalsIgnoreCase(rule.ruleType())) {
            return true;
        }
        JsonNode query = rule.executionRule().get("query");
        return query != null && query.isTextual() && query.asText().contains("{object}");
    }

    private static boolean hasPattern(JsonNode arguments) {
        return arguments != null && arguments.has("pattern") && arguments.get("pattern").isTextual()
                && !arguments.get("pattern").asText().isEmpty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
