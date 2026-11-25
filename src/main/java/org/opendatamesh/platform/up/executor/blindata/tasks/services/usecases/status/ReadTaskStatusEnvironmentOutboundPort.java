package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

interface ReadTaskStatusEnvironmentOutboundPort {

    Optional<EnvironmentProfile> findProfile(String environment);
}
