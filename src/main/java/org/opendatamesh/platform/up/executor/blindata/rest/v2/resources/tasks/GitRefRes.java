package org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Git ref of the task")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitRefRes {

    @Schema(description = "Release tag of the data product version (DevOps sends the tag of the version being deployed)")
    private String name;

    @Schema(description = "Type of the ref (TAG or BRANCH), ignored")
    private String type;

    public GitRefRes() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
