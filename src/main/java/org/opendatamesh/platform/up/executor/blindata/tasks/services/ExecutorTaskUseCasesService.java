package org.opendatamesh.platform.up.executor.blindata.tasks.services;

import jakarta.enterprise.context.ApplicationScoped;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ProviderRunId;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.RunStatus;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel.CancelTaskCommand;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.cancel.CancelTaskFactory;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs.ReadTaskLogsCommand;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs.ReadTaskLogsFactory;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.logs.ReadTaskLogsPresenter;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start.StartTaskCommand;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start.StartTaskFactory;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.start.StartTaskPresenter;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status.ReadTaskStatusCommand;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status.ReadTaskStatusFactory;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.usecases.status.ReadTaskStatusPresenter;

import java.time.Instant;

/** Runs the executor task use cases on domain types; version-independent. */
@ApplicationScoped
public class ExecutorTaskUseCasesService {

    private final StartTaskFactory startTaskFactory;
    private final ReadTaskStatusFactory readTaskStatusFactory;
    private final ReadTaskLogsFactory readTaskLogsFactory;
    private final CancelTaskFactory cancelTaskFactory;

    public ExecutorTaskUseCasesService(StartTaskFactory startTaskFactory, ReadTaskStatusFactory readTaskStatusFactory,
                                       ReadTaskLogsFactory readTaskLogsFactory, CancelTaskFactory cancelTaskFactory) {
        this.startTaskFactory = startTaskFactory;
        this.readTaskStatusFactory = readTaskStatusFactory;
        this.readTaskLogsFactory = readTaskLogsFactory;
        this.cancelTaskFactory = cancelTaskFactory;
    }

    public ProviderRunId start(StartTaskCommand command) {
        StartTaskResultHolder holder = new StartTaskResultHolder();
        startTaskFactory.buildStartTask(command, holder).execute();
        return holder.runId;
    }

    public TaskStatus status(ReadTaskStatusCommand command) {
        ReadTaskStatusResultHolder holder = new ReadTaskStatusResultHolder();
        readTaskStatusFactory.buildReadTaskStatus(command, holder).execute();
        return new TaskStatus(holder.providerRunId, holder.status);
    }

    public TaskLogs logs(ReadTaskLogsCommand command) {
        ReadTaskLogsResultHolder holder = new ReadTaskLogsResultHolder();
        readTaskLogsFactory.buildReadTaskLogs(command, holder).execute();
        return new TaskLogs(holder.content, holder.generatedAt);
    }

    public void cancel(CancelTaskCommand command) {
        cancelTaskFactory.buildCancelTask(command, runId -> { }).execute();
    }

    public record TaskStatus(String providerRunId, RunStatus status) {
    }

    public record TaskLogs(String content, Instant generatedAt) {
    }

    private static final class StartTaskResultHolder implements StartTaskPresenter {

        private ProviderRunId runId;

        @Override
        public void presentStarted(ProviderRunId providerRunId) {
            this.runId = providerRunId;
        }
    }

    private static final class ReadTaskStatusResultHolder implements ReadTaskStatusPresenter {

        private String providerRunId;
        private RunStatus status;

        @Override
        public void presentStatus(String providerRunId, RunStatus status) {
            this.providerRunId = providerRunId;
            this.status = status;
        }
    }

    private static final class ReadTaskLogsResultHolder implements ReadTaskLogsPresenter {

        private String content;
        private Instant generatedAt;

        @Override
        public void presentLogs(String content, Instant generatedAt) {
            this.content = content;
            this.generatedAt = generatedAt;
        }
    }
}
