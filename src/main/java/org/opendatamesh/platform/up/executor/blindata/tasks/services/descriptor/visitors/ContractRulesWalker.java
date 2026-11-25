package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.visitors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendatamesh.dpds.model.DataProductVersion;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;

/**
 * Walks a parsed descriptor with the visitors of this package, data product → port → datastore API → ODCS table, and
 * returns the contract rules the {@link ContractRulesCollector} built from the rules met.
 */
public final class ContractRulesWalker {

    private final ObjectMapper json;
    private final String blockingProperty;
    private final String connectionProperty;

    public ContractRulesWalker(ObjectMapper json, String blockingProperty, String connectionProperty) {
        this.json = json;
        this.blockingProperty = blockingProperty;
        this.connectionProperty = connectionProperty;
    }

    public ContractRules walk(DataProductVersion dataProductVersion) {
        ContractRulesCollector collector = new ContractRulesCollector(json, blockingProperty);
        DataProductRulesVisitor visitor = new DataProductRulesVisitor(collector, connectionProperty);
        if (dataProductVersion.getInfo() != null) {
            dataProductVersion.getInfo().accept(visitor);
        }
        if (dataProductVersion.getInterfaceComponents() != null) {
            dataProductVersion.getInterfaceComponents().accept(visitor);
        }
        return collector.result();
    }
}
