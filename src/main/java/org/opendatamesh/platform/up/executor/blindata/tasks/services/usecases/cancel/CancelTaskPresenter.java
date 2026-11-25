package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCasePresenter;

public interface CancelTaskPresenter extends UseCasePresenter {

    void presentCancelAccepted(ProviderRunId providerRunId);
}
