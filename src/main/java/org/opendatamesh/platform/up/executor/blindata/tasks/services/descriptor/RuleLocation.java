package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

/** Where a rule is declared: its port and connection, and the physical binding of its table or column. */
record RuleLocation(String portFqn, String connectionName, String schema, String object, String property) {

    RuleLocation onColumn(String column) {
        return new RuleLocation(portFqn, connectionName, schema, object, column);
    }
}
