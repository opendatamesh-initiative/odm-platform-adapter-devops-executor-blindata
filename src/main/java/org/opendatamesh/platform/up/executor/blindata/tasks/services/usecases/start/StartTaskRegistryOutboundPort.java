package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;

import java.util.Optional;

interface StartTaskRegistryOutboundPort {

    Optional<DataProductVersionDescriptor> findVersion(String dataProductFqn, String versionTag, String versionNumber);
}
