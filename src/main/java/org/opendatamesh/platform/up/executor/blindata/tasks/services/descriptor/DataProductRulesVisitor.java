package org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor;

import org.opendatamesh.dpds.model.blueprint.Blueprint;
import org.opendatamesh.dpds.model.components.Components;
import org.opendatamesh.dpds.model.core.ExternalDocs;
import org.opendatamesh.dpds.model.info.Info;
import org.opendatamesh.dpds.model.interfaces.InterfaceComponents;
import org.opendatamesh.dpds.model.interfaces.Port;
import org.opendatamesh.dpds.model.internals.InternalComponents;
import org.opendatamesh.dpds.visitors.DataProductVersionVisitor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Root of the walk: the data product's identity (for the check codes), then every port of the interface components. */
final class DataProductRulesVisitor implements DataProductVersionVisitor {

    private final ContractRulesCollector collector;
    private final PortRulesVisitor portVisitor;

    DataProductRulesVisitor(ContractRulesCollector collector, String connectionProperty) {
        this.collector = collector;
        this.portVisitor = new PortRulesVisitor(collector, connectionProperty);
    }

    @Override
    public void visit(Info info) {
        collector.dataProduct(info.getDomain(), info.getName());
    }

    @Override
    public void visit(InterfaceComponents interfaceComponents) {
        Stream.of(interfaceComponents.getInputPorts(), interfaceComponents.getOutputPorts(),
                        interfaceComponents.getControlPorts(), interfaceComponents.getDiscoveryPorts(),
                        interfaceComponents.getObservabilityPorts())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .forEach(port -> port.accept(portVisitor));
    }

    @Override
    public void visit(InternalComponents internalComponents) {
        // No contract rules in internal components.
    }

    @Override
    public void visit(Components components) {
        // Reusable components are read where they are used.
    }

    @Override
    public void visit(ExternalDocs externalDocs) {
        // Not part of the contract.
    }

    @Override
    public void visit(Blueprint blueprint) {
        // Not part of the contract.
    }
}
