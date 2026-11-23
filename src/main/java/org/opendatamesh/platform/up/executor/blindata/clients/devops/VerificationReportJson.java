package org.opendatamesh.platform.up.executor.blindata.clients.devops;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.RuleReport;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.SkippedRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;

import java.time.Instant;

/** JSON form of the report, as the DevOps Results tab and the task-level policies read it. */
final class VerificationReportJson {

    private final ObjectMapper json;

    VerificationReportJson(ObjectMapper json) {
        this.json = json;
    }

    ObjectNode toJson(VerificationReport report) {
        ObjectNode root = json.createObjectNode();
        root.put("schema", VerificationReport.SCHEMA);
        root.put("dataProduct", report.context().dataProductFqn());
        root.put("version", report.context().dataProductVersion());
        root.put("environment", report.context().environment());
        ObjectNode agentRun = root.putObject("agentRun");
        agentRun.put("sequenceId", report.sequenceId());
        agentRun.put("runStatus", report.agentRunStatus());
        agentRun.put("startedAt", iso(report.startedAt()));
        agentRun.put("finishedAt", iso(report.finishedAt()));
        root.put("verdict", report.verdict().name());
        ObjectNode summary = root.putObject("summary");
        summary.put("total", report.summary().total());
        summary.put("passed", report.summary().passed());
        summary.put("failed", report.summary().failed());
        summary.put("error", report.summary().error());
        summary.put("skipped", report.summary().skipped());
        summary.put("failedBlocking", report.summary().failedBlocking());
        ArrayNode rules = root.putArray("rules");
        report.rules().forEach(rule -> rules.add(rule(rule)));
        ArrayNode skipped = root.putArray("skipped");
        report.skipped().forEach(rule -> skipped.add(skipped(rule)));
        ArrayNode notes = root.putArray("notes");
        report.notes().forEach(notes::add);
        return root;
    }

    private ObjectNode rule(RuleReport rule) {
        ObjectNode node = json.createObjectNode();
        node.put("checkCode", rule.checkCode());
        node.put("ruleId", rule.ruleId());
        node.put("name", rule.name());
        node.put("port", rule.port());
        node.put("object", rule.object());
        node.put("property", rule.property());
        node.put("kind", rule.kind().name());
        node.put("blocking", rule.blocking());
        node.put("outcome", rule.outcome().name());
        node.put("metric", rule.metric());
        node.put("totalElements", rule.totalElements());
        node.put("score", rule.score());
        node.put("error", rule.error());
        node.put("note", rule.note());
        return node;
    }

    private ObjectNode skipped(SkippedRule rule) {
        ObjectNode node = json.createObjectNode();
        node.put("checkCode", rule.checkCode());
        node.put("ruleId", rule.ruleId());
        node.put("name", rule.ruleName());
        node.put("port", rule.portFqn());
        node.put("object", rule.object());
        node.put("property", rule.property());
        node.put("blocking", rule.blocking());
        node.put("reason", rule.reason());
        return node;
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
