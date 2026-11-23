package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import org.opendatamesh.dpds.visitors.core.ComponentBaseVisitor;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.OdcsPropertyDefinition;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.OdcsTableDefinition;

/** The ODCS contract of one table: its table-level rules, then the rules of each column (bound to that column). */
final class OdcsTableRulesVisitor implements ComponentBaseVisitor<OdcsTableDefinition> {

    private final ContractRulesCollector collector;
    private final RuleLocation table;

    OdcsTableRulesVisitor(ContractRulesCollector collector, RuleLocation table) {
        this.collector = collector;
        this.table = table;
    }

    @Override
    public void visit(OdcsTableDefinition definition) {
        if (definition.getQuality() != null) {
            definition.getQuality().forEach(rule -> collector.rule(rule, table));
        }
        if (definition.getProperties() != null) {
            definition.getProperties().forEach(this::visitColumn);
        }
    }

    private void visitColumn(String key, OdcsPropertyDefinition column) {
        if (column == null || column.getQuality() == null) {
            return;
        }
        RuleLocation location = table.onColumn(column.getName() != null && !column.getName().isBlank() ? column.getName() : key);
        column.getQuality().forEach(rule -> collector.rule(rule, location));
    }
}
