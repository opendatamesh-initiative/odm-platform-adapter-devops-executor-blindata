package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.RuleReport;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.SkippedRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.VerificationReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Plain-text form of the verification report for the task logs: header, one line per rule, skipped rules, agent log. */
class ReportTableRenderer {

    private static final String[] HEADER = {"OUTCOME", "BLOCKING", "RULE", "OBJECT", "METRIC", "TOTAL", "DETAIL"};

    String render(VerificationReport report, String agentLog) {
        StringBuilder out = new StringBuilder();
        header(report, out);
        rules(report.rules(), out);
        skipped(report.skipped(), out);
        notes(report.notes(), out);
        out.append("\n=== Agent job log ===\n").append(agentLog == null || agentLog.isBlank() ? "(empty)" : agentLog);
        if (!out.toString().endsWith("\n")) {
            out.append('\n');
        }
        return out.toString();
    }

    private static void header(VerificationReport report, StringBuilder out) {
        out.append("=== Contract verification: ").append(report.verdict()).append(" ===\n");
        out.append("Data product: ").append(report.context().dataProductFqn())
                .append("  version: ").append(report.context().dataProductVersion())
                .append("  environment: ").append(report.context().environment()).append('\n');
        out.append("Agent job: ").append(report.sequenceId()).append(" (").append(report.agentRunStatus()).append(")\n");
        out.append(String.format(Locale.ROOT, "Rules: %d total, %d passed, %d failed, %d error, %d skipped; %d blocking failure(s)%n",
                report.summary().total(), report.summary().passed(), report.summary().failed(),
                report.summary().error(), report.summary().skipped(), report.summary().failedBlocking()));
    }

    private static void rules(List<RuleReport> rules, StringBuilder out) {
        if (rules.isEmpty()) {
            return;
        }
        List<String[]> rows = new ArrayList<>();
        rows.add(HEADER);
        for (RuleReport rule : rules) {
            rows.add(new String[]{rule.outcome().name(), rule.blocking() ? "yes" : "no", rule.checkCode(),
                    object(rule.object(), rule.property()), number(rule.metric()), number(rule.totalElements()), detail(rule)});
        }
        out.append('\n');
        table(rows, out);
    }

    private static void skipped(List<SkippedRule> skipped, StringBuilder out) {
        if (skipped.isEmpty()) {
            return;
        }
        out.append("\nSkipped rules:\n");
        for (SkippedRule rule : skipped) {
            out.append("  - ").append(rule.ruleId() == null ? rule.ruleName() : rule.ruleId())
                    .append(" on ").append(object(rule.object(), rule.property()))
                    .append(rule.blocking() ? " (blocking)" : " (non-blocking)")
                    .append(": ").append(rule.reason()).append('\n');
        }
    }

    private static void notes(List<String> notes, StringBuilder out) {
        for (String note : notes) {
            out.append("\nNote: ").append(note).append('\n');
        }
    }

    private static void table(List<String[]> rows, StringBuilder out) {
        int[] widths = new int[HEADER.length];
        for (String[] row : rows) {
            for (int i = 0; i < row.length - 1; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        for (String[] row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                line.append(i == row.length - 1 ? row[i] : pad(row[i], widths[i] + 2));
            }
            out.append(line.toString().stripTrailing()).append('\n');
        }
    }

    private static String detail(RuleReport rule) {
        List<String> parts = new ArrayList<>();
        if (rule.error() != null) {
            parts.add(rule.error());
        }
        if (rule.note() != null) {
            parts.add(rule.note());
        }
        return String.join("; ", parts);
    }

    private static String object(String object, String property) {
        if (object == null) {
            return "-";
        }
        return property == null ? object : object + "." + property;
    }

    private static String number(Double value) {
        if (value == null) {
            return "-";
        }
        return value == Math.rint(value) ? String.valueOf(value.longValue()) : String.valueOf(value);
    }

    private static String pad(String value, int width) {
        return value + " ".repeat(Math.max(1, width - value.length()));
    }
}
