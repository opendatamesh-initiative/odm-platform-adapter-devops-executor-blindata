package org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.start;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.Map;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.ExecutorParametersRes;

@Schema(description = "Start of a contract verification")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStartCommandRes {

    @Schema(description = "Executor parameters of the task")
    private ExecutorParametersRes executorParameters;

    @Schema(description = "dataProductFqn and environment (required), dataProductVersion, activityName and taskName (optional)")
    private Map<String, String> pipelineParameters;

    public TaskStartCommandRes() {
    }

    public ExecutorParametersRes getExecutorParameters() {
        return executorParameters;
    }

    public void setExecutorParameters(ExecutorParametersRes executorParameters) {
        this.executorParameters = executorParameters;
    }

    public Map<String, String> getPipelineParameters() {
        return pipelineParameters;
    }

    public void setPipelineParameters(Map<String, String> pipelineParameters) {
        this.pipelineParameters = pipelineParameters;
    }
}
