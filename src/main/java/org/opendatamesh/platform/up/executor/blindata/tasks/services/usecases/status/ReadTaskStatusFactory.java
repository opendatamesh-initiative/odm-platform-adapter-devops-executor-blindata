package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentClient;
import org.opendatamesh.platform.up.executor.blindata.clients.devops.DevOpsClient;
import org.opendatamesh.platform.up.executor.blindata.config.EnvironmentProfiles;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCase;

@ApplicationScoped
public class ReadTaskStatusFactory {

    private final EnvironmentProfiles profiles;
    private final AgentClient agent;
    private final DevOpsClient devOps;

    public ReadTaskStatusFactory(EnvironmentProfiles profiles, AgentClient agent, DevOpsClient devOps) {
        this.profiles = profiles;
        this.agent = agent;
        this.devOps = devOps;
    }

    public UseCase buildReadTaskStatus(ReadTaskStatusCommand command, ReadTaskStatusPresenter presenter) {
        return new ReadTaskStatus(command, presenter,
                new ReadTaskStatusEnvironmentOutboundPortImpl(profiles),
                new ReadTaskStatusAgentOutboundPortImpl(agent),
                new ReadTaskStatusResultsOutboundPortImpl(devOps));
    }
}
