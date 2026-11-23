package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentClient;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

class ReadTaskLogsAgentOutboundPortImpl implements ReadTaskLogsAgentOutboundPort {

    private final AgentClient agent;

    ReadTaskLogsAgentOutboundPortImpl(AgentClient agent) {
        this.agent = agent;
    }

    @Override
    public Optional<AgentRun> readRun(EnvironmentProfile profile, long sequenceId) {
        return agent.readRun(profile, sequenceId);
    }

    @Override
    public Optional<String> readLog(EnvironmentProfile profile, long sequenceId) {
        return agent.readLog(profile, sequenceId);
    }
}
