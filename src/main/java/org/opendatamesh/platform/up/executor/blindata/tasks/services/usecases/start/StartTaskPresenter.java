package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCasePresenter;

public interface StartTaskPresenter extends UseCasePresenter {

    void presentStarted(ProviderRunId providerRunId);
}
