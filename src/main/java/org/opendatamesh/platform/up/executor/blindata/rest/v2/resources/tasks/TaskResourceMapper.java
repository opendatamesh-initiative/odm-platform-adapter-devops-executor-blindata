package org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.exceptions.BadRequestException;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.logs.TaskLogsRes;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.start.TaskStartCommandRes;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.start.TaskStartResultRes;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.status.TaskStatusRes;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.ExecutorTaskUseCasesService.TaskLogs;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.ExecutorTaskUseCasesService.TaskStatus;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel.CancelTaskCommand;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs.ReadTaskLogsCommand;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start.StartTaskCommand;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status.ReadTaskStatusCommand;

import java.util.Date;
import java.util.Map;

/** Maps the DevOps executor API v2 resources to the use case commands and back. */
@ApplicationScoped
public class TaskResourceMapper {

    public static final String DATA_PRODUCT_FQN = "dataProductFqn";
    public static final String ENVIRONMENT = "environment";
    public static final String DATA_PRODUCT_VERSION = "dataProductVersion";
    public static final String ACTIVITY_NAME = "activityName";
    public static final String TASK_NAME = "taskName";

    private static final String EXECUTOR_PARAMETERS_REQUIRED = "executorParameters is required";
    private static final String PROVIDER_RUN_ID_REQUIRED = "providerRunId is required";

    public StartTaskCommand toStartCommand(TaskStartCommandRes body) {
        if (body == null || body.getExecutorParameters() == null) {
            throw new BadRequestException(EXECUTOR_PARAMETERS_REQUIRED);
        }
        Map<String, String> parameters = body.getPipelineParameters() == null ? Map.of() : body.getPipelineParameters();
        GitRefRes ref = body.getExecutorParameters().getRef();
        return new StartTaskCommand(parameters.get(DATA_PRODUCT_FQN), parameters.get(ENVIRONMENT),
                ref == null ? null : ref.getName(), parameters.get(DATA_PRODUCT_VERSION),
                parameters.get(ACTIVITY_NAME), parameters.get(TASK_NAME));
    }

    public ReadTaskStatusCommand toStatusCommand(TaskRunCommandRes body) {
        return new ReadTaskStatusCommand(requireRunId(body));
    }

    public ReadTaskLogsCommand toLogsCommand(TaskRunCommandRes body) {
        return new ReadTaskLogsCommand(requireRunId(body));
    }

    public CancelTaskCommand toCancelCommand(TaskRunCommandRes body) {
        return new CancelTaskCommand(requireRunId(body));
    }

    public TaskStartResultRes toStartResult(ProviderRunId runId) {
        TaskStartResultRes res = new TaskStartResultRes();
        res.setProviderRunId(runId.value());
        return res;
    }

    public TaskStatusRes toStatusRes(TaskStatus status) {
        TaskStatusRes res = new TaskStatusRes();
        res.setProviderRunId(status.providerRunId());
        res.setStatus(status.status().name());
        return res;
    }

    public TaskLogsRes toLogsRes(TaskLogs logs) {
        TaskLogsRes res = new TaskLogsRes();
        res.setContent(logs.content());
        res.setGeneratedAt(Date.from(logs.generatedAt()));
        return res;
    }

    private static String requireRunId(TaskRunCommandRes body) {
        if (body == null || body.getExecutorParameters() == null) {
            throw new BadRequestException(EXECUTOR_PARAMETERS_REQUIRED);
        }
        if (body.getProviderRunId() == null || body.getProviderRunId().isBlank()) {
            throw new BadRequestException(PROVIDER_RUN_ID_REQUIRED);
        }
        return body.getProviderRunId();
    }
}
