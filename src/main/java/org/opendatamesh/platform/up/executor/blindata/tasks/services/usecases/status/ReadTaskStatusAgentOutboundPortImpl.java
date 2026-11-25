package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentClient;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

class ReadTaskStatusAgentOutboundPortImpl implements ReadTaskStatusAgentOutboundPort {

    private final AgentClient agent;

    ReadTaskStatusAgentOutboundPortImpl(AgentClient agent) {
        this.agent = agent;
    }

    @Override
    public Optional<AgentRun> readRun(EnvironmentProfile profile, long sequenceId) {
        return agent.readRun(profile, sequenceId);
    }
}
