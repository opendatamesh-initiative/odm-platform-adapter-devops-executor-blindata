package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentClient;
import org.opendatamesh.platform.up.executor.blindata.clients.registry.RegistryClient;
import org.opendatamesh.platform.up.executor.blindata.config.EnvironmentProfiles;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.DescriptorContractRulesReader;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCase;

@ApplicationScoped
public class StartTaskFactory {

    private final EnvironmentProfiles profiles;
    private final RegistryClient registry;
    private final DescriptorContractRulesReader reader;
    private final AgentClient agent;

    public StartTaskFactory(EnvironmentProfiles profiles, RegistryClient registry, DescriptorContractRulesReader reader,
                            AgentClient agent) {
        this.profiles = profiles;
        this.registry = registry;
        this.reader = reader;
        this.agent = agent;
    }

    public UseCase buildStartTask(StartTaskCommand command, StartTaskPresenter presenter) {
        return new StartTask(command, presenter,
                new StartTaskEnvironmentOutboundPortImpl(profiles),
                new StartTaskRegistryOutboundPortImpl(registry),
                new StartTaskRulesOutboundPortImpl(reader),
                new StartTaskAgentOutboundPortImpl(agent));
    }
}
