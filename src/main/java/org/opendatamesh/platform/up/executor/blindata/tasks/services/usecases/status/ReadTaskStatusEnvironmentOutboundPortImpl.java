package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.blindata.config.EnvironmentProfiles;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

class ReadTaskStatusEnvironmentOutboundPortImpl implements ReadTaskStatusEnvironmentOutboundPort {

    private final EnvironmentProfiles profiles;

    ReadTaskStatusEnvironmentOutboundPortImpl(EnvironmentProfiles profiles) {
        this.profiles = profiles;
    }

    @Override
    public Optional<EnvironmentProfile> findProfile(String environment) {
        return profiles.find(environment);
    }
}
