package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentClient;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractJob;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

class StartTaskAgentOutboundPortImpl implements StartTaskAgentOutboundPort {

    private final AgentClient agent;

    StartTaskAgentOutboundPortImpl(AgentClient agent) {
        this.agent = agent;
    }

    @Override
    public long launch(EnvironmentProfile profile, ContractJob job) {
        return agent.launch(profile, job);
    }
}
