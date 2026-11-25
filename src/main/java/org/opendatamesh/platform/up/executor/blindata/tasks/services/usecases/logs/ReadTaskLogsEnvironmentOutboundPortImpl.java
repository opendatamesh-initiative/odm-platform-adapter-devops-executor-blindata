package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.blindata.config.EnvironmentProfiles;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

class ReadTaskLogsEnvironmentOutboundPortImpl implements ReadTaskLogsEnvironmentOutboundPort {

    private final EnvironmentProfiles profiles;

    ReadTaskLogsEnvironmentOutboundPortImpl(EnvironmentProfiles profiles) {
        this.profiles = profiles;
    }

    @Override
    public Optional<EnvironmentProfile> findProfile(String environment) {
        return profiles.find(environment);
    }
}
