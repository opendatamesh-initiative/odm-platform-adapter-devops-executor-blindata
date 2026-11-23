package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

interface ReadTaskLogsEnvironmentOutboundPort {

    Optional<EnvironmentProfile> findProfile(String environment);
}
