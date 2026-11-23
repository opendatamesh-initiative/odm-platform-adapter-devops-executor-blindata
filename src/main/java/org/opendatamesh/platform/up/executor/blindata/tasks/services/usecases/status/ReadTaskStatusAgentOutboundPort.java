package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

interface ReadTaskStatusAgentOutboundPort {

    Optional<AgentRun> readRun(EnvironmentProfile profile, long sequenceId);
}
