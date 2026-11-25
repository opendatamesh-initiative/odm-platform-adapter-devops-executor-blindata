package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.clients.registry.RegistryClient;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import java.util.Optional;

class StartTaskRegistryOutboundPortImpl implements StartTaskRegistryOutboundPort {

    private final RegistryClient registry;

    StartTaskRegistryOutboundPortImpl(RegistryClient registry) {
        this.registry = registry;
    }

    @Override
    public Optional<DataProductVersionDescriptor> findVersion(String dataProductFqn, String versionTag, String versionNumber) {
        return registry.findVersion(dataProductFqn, versionTag, versionNumber);
    }
}
