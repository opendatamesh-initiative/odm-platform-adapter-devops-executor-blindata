package org.opendatamesh.platform.up.executor.blindata.clients.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentCheckResult;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRunStatus;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractJob;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRuleKind;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.SkippedRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationContext;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wire format of the verification job ({@code QUALITY_PROBES}, {@code CONTRACT_RULE} checks, TEST RULES parity) and of
 * the agent job history. The executor's own metadata travels in {@code odm} fields that the agent ignores when it runs
 * the job and keeps in {@code jobDetails.configs}, so the status call can read it back.
 */
final class AgentJobMapper {

    static final String JOB_TYPE = "QUALITY_PROBES";
    static final String PROBE_TYPE = "CONTRACT_RULE";
    static final String ENVELOPE_SCHEMA = "blindata.qualityProbe.contractRule.v1";
    static final String JOB_SCHEMA = "blindata.devops.contractVerification.job.v1";

    private final ObjectMapper json;

    AgentJobMapper(ObjectMapper json) {
        this.json = json;
    }

    ObjectNode toJob(ContractJob job) {
        ObjectNode configs = json.createObjectNode();
        configs.put("name", job.name());
        configs.set("odm", context(job.context()));
        ArrayNode checks = configs.putArray("checks");
        job.rules().forEach(rule -> checks.add(check(rule)));
        ArrayNode skipped = configs.putArray("skipped");
        job.skipped().forEach(rule -> skipped.add(skippedRule(rule)));
        configs.putObject("outputOptions").put("dryRun", true);
        ObjectNode body = json.createObjectNode();
        body.put("type", JOB_TYPE);
        body.set("configs", configs);
        return body;
    }

    AgentRun fromHistory(JsonNode history) {
        JsonNode configs = history.path("jobDetails").path("configs");
        String rawStatus = text(history, "runStatus");
        return new AgentRun(history.path("sequenceId").asLong(), AgentRunStatus.parse(rawStatus), rawStatus,
                instant(history.get("startedAt")), instant(history.get("finishedAt")),
                readContext(configs.get("odm")), readRules(configs.path("checks")), readSkipped(configs.path("skipped")),
                readResults(history.path("jobResults")));
    }

    private ObjectNode context(VerificationContext context) {
        ObjectNode odm = json.createObjectNode();
        odm.put("schema", JOB_SCHEMA);
        odm.put("dataProductFqn", context.dataProductFqn());
        odm.put("dataProductVersion", context.dataProductVersion());
        odm.put("versionTag", context.versionTag());
        odm.put("environment", context.environment());
        odm.put("activityName", context.activityName());
        odm.put("taskName", context.taskName());
        return odm;
    }

    private ObjectNode check(ContractRule rule) {
        ObjectNode query = json.createObjectNode();
        query.put("connectionName", rule.connectionName());
        query.put("schema", ENVELOPE_SCHEMA);
        query.set("rule", rule.executionRule().deepCopy());
        query.set("physicalBinding", physicalBinding(rule));
        ObjectNode check = json.createObjectNode();
        check.put("code", rule.checkCode());
        ObjectNode probe = check.putObject("probe");
        probe.put("type", PROBE_TYPE);
        probe.putArray("queries").add(query);
        check.set("odm", ruleMetadata(rule));
        return check;
    }

    private ObjectNode physicalBinding(ContractRule rule) {
        ObjectNode binding = json.createObjectNode();
        putIfText(binding, "schema", rule.schema());
        putIfText(binding, "object", rule.object());
        putIfText(binding, "property", rule.property());
        return binding;
    }

    private ObjectNode ruleMetadata(ContractRule rule) {
        ObjectNode odm = json.createObjectNode();
        odm.put("ruleId", rule.ruleId());
        odm.put("ruleName", rule.ruleName());
        odm.put("port", rule.portFqn());
        odm.put("ruleType", rule.ruleType());
        odm.put("blocking", rule.blocking());
        odm.put("kind", rule.kind().name());
        return odm;
    }

    private ObjectNode skippedRule(SkippedRule rule) {
        ObjectNode node = json.createObjectNode();
        node.put("checkCode", rule.checkCode());
        node.put("ruleId", rule.ruleId());
        node.put("ruleName", rule.ruleName());
        node.put("port", rule.portFqn());
        node.put("object", rule.object());
        node.put("property", rule.property());
        node.put("blocking", rule.blocking());
        node.put("reason", rule.reason());
        return node;
    }

    private static VerificationContext readContext(JsonNode odm) {
        if (odm == null || !JOB_SCHEMA.equals(text(odm, "schema"))) {
            return null;
        }
        return new VerificationContext(text(odm, "dataProductFqn"), text(odm, "dataProductVersion"),
                text(odm, "versionTag"), text(odm, "environment"), text(odm, "activityName"), text(odm, "taskName"));
    }

    private static List<ContractRule> readRules(JsonNode checks) {
        List<ContractRule> rules = new ArrayList<>();
        for (JsonNode check : checks) {
            JsonNode query = check.path("probe").path("queries").path(0);
            JsonNode binding = query.path("physicalBinding");
            JsonNode odm = check.path("odm");
            rules.add(new ContractRule(text(check, "code"), text(odm, "ruleId"), text(odm, "ruleName"),
                    text(odm, "port"), text(binding, "schema"), text(binding, "object"), text(binding, "property"),
                    text(query, "connectionName"), text(odm, "ruleType"), !odm.has("blocking") || odm.get("blocking").asBoolean(true),
                    kind(text(odm, "kind")), query.path("rule").deepCopy()));
        }
        return rules;
    }

    private static List<SkippedRule> readSkipped(JsonNode skipped) {
        List<SkippedRule> rules = new ArrayList<>();
        for (JsonNode rule : skipped) {
            rules.add(new SkippedRule(text(rule, "checkCode"), text(rule, "ruleId"), text(rule, "ruleName"),
                    text(rule, "port"), text(rule, "object"), text(rule, "property"),
                    rule.path("blocking").asBoolean(true), text(rule, "reason")));
        }
        return rules;
    }

    private static List<AgentCheckResult> readResults(JsonNode jobResults) {
        List<AgentCheckResult> rows = new ArrayList<>();
        for (String list : new String[]{"results", "errors"}) {
            for (JsonNode row : jobResults.path(list)) {
                rows.add(new AgentCheckResult(text(row, "qualityCheckCode"), number(row.get("metric")),
                        number(row.get("totalElements")), integer(row.get("score")), text(row, "error")));
            }
        }
        return rows;
    }

    private static ContractRuleKind kind(String value) {
        return ContractRuleKind.STRUCTURE.name().equals(value) ? ContractRuleKind.STRUCTURE : ContractRuleKind.RULE;
    }

    private static Instant instant(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return Instant.ofEpochMilli(value.asLong());
        }
        try {
            return OffsetDateTime.parse(value.asText()).toInstant();
        } catch (DateTimeParseException e) {
            try {
                return Instant.parse(value.asText());
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private static Double number(JsonNode value) {
        return value != null && value.isNumber() ? value.asDouble() : null;
    }

    private static Integer integer(JsonNode value) {
        return value != null && value.isNumber() ? value.asInt() : null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && !value.isNull() && !value.isMissingNode() ? value.asText() : null;
    }

    private static void putIfText(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value);
        }
    }
}
