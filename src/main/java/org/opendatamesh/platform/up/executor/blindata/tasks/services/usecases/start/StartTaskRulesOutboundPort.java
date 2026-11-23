package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRule;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractRules;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.DataProductVersionDescriptor;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.Platform;

import java.util.Optional;

interface StartTaskRulesOutboundPort {

    ContractRules readRules(DataProductVersionDescriptor descriptor);

    /** Why the agent cannot run the rule on the platform, or empty when it can. */
    Optional<String> unsupportedReason(ContractRule rule, Platform platform);
}
