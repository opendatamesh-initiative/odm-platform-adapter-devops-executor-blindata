package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.clients.agent.AgentRuleSupport;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.Platform;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.descriptor.DescriptorContractRulesReader;
import java.util.Optional;

class StartTaskRulesOutboundPortImpl implements StartTaskRulesOutboundPort {

    private final DescriptorContractRulesReader reader;

    StartTaskRulesOutboundPortImpl(DescriptorContractRulesReader reader) {
        this.reader = reader;
    }

    @Override
    public ContractRules readRules(DataProductVersionDescriptor descriptor) {
        return reader.read(descriptor);
    }

    @Override
    public Optional<String> unsupportedReason(ContractRule rule, Platform platform) {
        return AgentRuleSupport.unsupportedReason(rule, platform);
    }
}
