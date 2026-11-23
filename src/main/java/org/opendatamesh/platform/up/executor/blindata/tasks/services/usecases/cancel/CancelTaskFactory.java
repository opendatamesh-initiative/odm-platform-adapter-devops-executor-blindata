package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentClient;
import org.opendatamesh.platform.up.executor.blindata.config.EnvironmentProfiles;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCase;

@ApplicationScoped
public class CancelTaskFactory {

    private final EnvironmentProfiles profiles;
    private final AgentClient agent;

    public CancelTaskFactory(EnvironmentProfiles profiles, AgentClient agent) {
        this.profiles = profiles;
        this.agent = agent;
    }

    public UseCase buildCancelTask(CancelTaskCommand command, CancelTaskPresenter presenter) {
        return new CancelTask(command, presenter,
                new CancelTaskEnvironmentOutboundPortImpl(profiles),
                new CancelTaskAgentOutboundPortImpl(agent));
    }
}
