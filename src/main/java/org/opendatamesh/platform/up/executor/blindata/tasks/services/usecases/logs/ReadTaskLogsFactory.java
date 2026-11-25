package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentClient;
import org.opendatamesh.platform.up.executor.blindata.config.EnvironmentProfiles;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCase;

@ApplicationScoped
public class ReadTaskLogsFactory {

    private final EnvironmentProfiles profiles;
    private final AgentClient agent;

    public ReadTaskLogsFactory(EnvironmentProfiles profiles, AgentClient agent) {
        this.profiles = profiles;
        this.agent = agent;
    }

    public UseCase buildReadTaskLogs(ReadTaskLogsCommand command, ReadTaskLogsPresenter presenter) {
        return new ReadTaskLogs(command, presenter,
                new ReadTaskLogsEnvironmentOutboundPortImpl(profiles),
                new ReadTaskLogsAgentOutboundPortImpl(agent),
                new ReadTaskLogsRendererOutboundPortImpl());
    }
}
