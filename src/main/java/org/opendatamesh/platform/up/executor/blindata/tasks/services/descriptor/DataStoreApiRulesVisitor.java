package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApi;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiDatabaseService;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiInfo;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiSchema;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApiStandardDefinitionObject;
import org.opendatamesh.dpds.datastoreapi.v1.visitor.DataStoreApiSchemaVisitor;
import org.opendatamesh.dpds.datastoreapi.v1.visitor.DataStoreApiVisitor;
import org.opendatamesh.dpds.model.core.ComponentBase;
import org.opendatamesh.dpds.model.core.ExternalDocs;
import org.opendatamesh.dpds.visitors.core.StandardDefinitionVisitor;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.OdcsTableDefinition;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.odcs.OdcsTableDefinitionConverter;

/**
 * The datastore API of a port: its schema ({@code databaseSchemaName} is the binding schema) and each table. A table
 * whose definition the ODCS plugin read is walked for its rules; one of another specification that still declares
 * {@code quality} rules is reported, so its rules are not silently ignored.
 */
final class DataStoreApiRulesVisitor implements StandardDefinitionVisitor<DataStoreApi> {

    private static final String QUALITY = "quality";

    private final ContractRulesCollector collector;
    private final String portFqn;
    private final String connectionName;
    private String schemaName;

    DataStoreApiRulesVisitor(ContractRulesCollector collector, String portFqn, String connectionName) {
        this.collector = collector;
        this.portFqn = portFqn;
        this.connectionName = connectionName;
    }

    @Override
    public void visit(DataStoreApi dataStoreApi) {
        if (dataStoreApi.getSchema() != null) {
            dataStoreApi.getSchema().accept(new SchemaVisitor());
        }
    }

    @Override
    public void visit(ExternalDocs externalDocs) {
        // Not part of the contract rules.
    }

    /** The schema and its tables: the datastore API's own visitor family. */
    private final class SchemaVisitor implements DataStoreApiVisitor, DataStoreApiSchemaVisitor {

        @Override
        public void visit(DataStoreApiSchema schema) {
            schemaName = schema.getDatabaseSchemaName();
            if (schema.getTables() != null) {
                schema.getTables().forEach(table -> table.accept(this));
            }
        }

        @Override
        public void visit(DataStoreApiStandardDefinitionObject table) {
            ComponentBase definition = table.getDefinition();
            if (definition instanceof OdcsTableDefinition odcsTable) {
                String object = odcsTable.getName() != null ? odcsTable.getName() : table.getName();
                odcsTable.accept(new OdcsTableRulesVisitor(collector,
                        new RuleLocation(portFqn, connectionName, schemaName, object, null)));
            } else if (definition != null && definition.getAdditionalProperties().containsKey(QUALITY)) {
                collector.unreadTable(new RuleLocation(portFqn, connectionName, schemaName, table.getName(), null),
                        "the rules of table " + table.getName() + " are not read: its definition is "
                                + table.getSpecification() + " " + table.getSpecificationVersion() + ", not "
                                + OdcsTableDefinitionConverter.SPECIFICATION + " 1.x");
            }
        }

        @Override
        public void visit(DataStoreApiInfo info) {
            // Not part of the contract rules.
        }

        @Override
        public void visit(DataStoreApiDatabaseService databaseService) {
            // The connection comes from x-blindataConnectionName, not from the datastore API services.
        }
    }
}
