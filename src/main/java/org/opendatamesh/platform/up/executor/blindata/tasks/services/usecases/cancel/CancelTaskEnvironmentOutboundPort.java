package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

interface CancelTaskEnvironmentOutboundPort {

    Optional<EnvironmentProfile> findProfile(String environment);
}
