package org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.RunStatus;
import org.opendatamesh.platform.up.executor.blindata.utils.usecases.UseCasePresenter;

public interface ReadTaskStatusPresenter extends UseCasePresenter {

    void presentStatus(String providerRunId, RunStatus status);
}
