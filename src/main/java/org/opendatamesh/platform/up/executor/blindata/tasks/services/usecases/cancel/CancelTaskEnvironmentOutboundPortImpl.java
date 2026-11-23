package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.blindata.config.EnvironmentProfiles;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

class CancelTaskEnvironmentOutboundPortImpl implements CancelTaskEnvironmentOutboundPort {

    private final EnvironmentProfiles profiles;

    CancelTaskEnvironmentOutboundPortImpl(EnvironmentProfiles profiles) {
        this.profiles = profiles;
    }

    @Override
    public Optional<EnvironmentProfile> findProfile(String environment) {
        return profiles.find(environment);
    }
}
