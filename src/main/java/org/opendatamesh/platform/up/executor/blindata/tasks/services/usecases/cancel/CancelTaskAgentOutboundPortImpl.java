package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentClient;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

class CancelTaskAgentOutboundPortImpl implements CancelTaskAgentOutboundPort {

    private final AgentClient agent;

    CancelTaskAgentOutboundPortImpl(AgentClient agent) {
        this.agent = agent;
    }

    @Override
    public Optional<AgentRun> readRun(EnvironmentProfile profile, long sequenceId) {
        return agent.readRun(profile, sequenceId);
    }
}
