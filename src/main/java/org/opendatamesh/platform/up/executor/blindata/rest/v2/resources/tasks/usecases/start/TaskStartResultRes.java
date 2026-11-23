package org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.start;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Result of the start")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskStartResultRes {

    @Schema(description = "Run id: <environment>:<agent job sequence id>")
    private String providerRunId;

    public TaskStartResultRes() {
    }

    public String getProviderRunId() {
        return providerRunId;
    }

    public void setProviderRunId(String providerRunId) {
        this.providerRunId = providerRunId;
    }
}
