package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractJob;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

interface StartTaskAgentOutboundPort {

    long launch(EnvironmentProfile profile, ContractJob job);
}
