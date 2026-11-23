package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import com.fasterxml.jackson.databind.JsonNode;
import org.opendatamesh.dpds.datastoreapi.v1.model.DataStoreApi;
import org.opendatamesh.dpds.model.core.ComponentBase;
import org.opendatamesh.dpds.model.core.ExternalDocs;
import org.opendatamesh.dpds.model.core.StandardDefinition;
import org.opendatamesh.dpds.model.interfaces.Expectations;
import org.opendatamesh.dpds.model.interfaces.Obligations;
import org.opendatamesh.dpds.model.interfaces.Port;
import org.opendatamesh.dpds.model.interfaces.Promises;
import org.opendatamesh.dpds.visitors.interfaces.InterfaceComponentsVisitor;
import org.opendatamesh.dpds.visitors.interfaces.port.PortVisitor;
import org.opendatamesh.dpds.visitors.interfaces.port.PromisesVisitor;

/**
 * One port: its Blindata connection name ({@code x-blindataConnectionName}), then its promised API. Only a datastore
 * API definition (already a {@link DataStoreApi} thanks to the parser plugin) carries contract rules.
 */
final class PortRulesVisitor implements InterfaceComponentsVisitor, PortVisitor, PromisesVisitor {

    private final ContractRulesCollector collector;
    private final String connectionProperty;
    private Port port;

    PortRulesVisitor(ContractRulesCollector collector, String connectionProperty) {
        this.collector = collector;
        this.connectionProperty = connectionProperty;
    }

    @Override
    public void visit(Port port) {
        this.port = port;
        if (port.getPromises() != null) {
            port.getPromises().accept(this);
        }
    }

    @Override
    public void visit(Promises promises) {
        if (promises.getApi() != null) {
            promises.getApi().accept(this);
        }
    }

    @Override
    public void visit(StandardDefinition api) {
        ComponentBase definition = api.getDefinition();
        if (definition instanceof DataStoreApi) {
            definition.accept(new DataStoreApiRulesVisitor(collector, port.getFullyQualifiedName(), connectionName()));
        }
    }

    @Override
    public void visit(Obligations obligations) {
        // Not part of the contract rules.
    }

    @Override
    public void visit(Expectations expectations) {
        // Not part of the contract rules.
    }

    @Override
    public void visit(ExternalDocs externalDocs) {
        // Not part of the contract rules.
    }

    /** The configured key, or the same key without the {@code x-} prefix (observer parity). */
    private String connectionName() {
        String name = textProperty(connectionProperty);
        if (name == null && connectionProperty.startsWith("x-")) {
            name = textProperty(connectionProperty.substring(2));
        }
        return name;
    }

    private String textProperty(String key) {
        JsonNode value = port.getAdditionalProperties().get(key);
        return value != null && value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }
}
