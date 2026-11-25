package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

interface StartTaskEnvironmentOutboundPort {

    Optional<EnvironmentProfile> findProfile(String environment);
}
