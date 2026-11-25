package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

interface CancelTaskAgentOutboundPort {

    Optional<AgentRun> readRun(EnvironmentProfile profile, long sequenceId);
}
