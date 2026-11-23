package org.opendatamesh.platform.up.executor.blindata.clients.registry;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;

import java.util.Optional;

/** ODM Registry v2: the descriptor of a data product version. */
public interface RegistryClient {

    /**
     * Finds the version of the data product {@code dataProductFqn} by version number when given, else by tag.
     * Empty when the data product or the version does not exist.
     */
    Optional<DataProductVersionDescriptor> findVersion(String dataProductFqn, String tag, String versionNumber);
}
