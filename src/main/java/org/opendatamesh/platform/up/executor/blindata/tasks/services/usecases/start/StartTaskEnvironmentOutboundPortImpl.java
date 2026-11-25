package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.config.EnvironmentProfiles;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;
import java.util.Optional;

class StartTaskEnvironmentOutboundPortImpl implements StartTaskEnvironmentOutboundPort {

    private final EnvironmentProfiles profiles;

    StartTaskEnvironmentOutboundPortImpl(EnvironmentProfiles profiles) {
        this.profiles = profiles;
    }

    @Override
    public Optional<EnvironmentProfile> findProfile(String environment) {
        return profiles.find(environment);
    }
}
