package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

interface ReadTaskLogsAgentOutboundPort {

    Optional<AgentRun> readRun(EnvironmentProfile profile, long sequenceId);

    Optional<String> readLog(EnvironmentProfile profile, long sequenceId);
}
