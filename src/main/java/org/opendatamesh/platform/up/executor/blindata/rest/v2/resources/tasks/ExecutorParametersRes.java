package org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Executor parameters of the task")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutorParametersRes {

    @Schema(description = "Key of the repository in the DevOps platform, ignored")
    private String repositoryKey;

    @Schema(description = "Repository of the data product, ignored")
    private DataProductRepoRes dataProductRepo;

    @Schema(description = "Git ref: its name is the release tag of the version to verify")
    private GitRefRes ref;

    @Schema(description = "Ignored by the Blindata executor")
    private String pipelineIdentifier;

    public ExecutorParametersRes() {
    }

    public String getRepositoryKey() {
        return repositoryKey;
    }

    public void setRepositoryKey(String repositoryKey) {
        this.repositoryKey = repositoryKey;
    }

    public DataProductRepoRes getDataProductRepo() {
        return dataProductRepo;
    }

    public void setDataProductRepo(DataProductRepoRes dataProductRepo) {
        this.dataProductRepo = dataProductRepo;
    }

    public GitRefRes getRef() {
        return ref;
    }

    public void setRef(GitRefRes ref) {
        this.ref = ref;
    }

    public String getPipelineIdentifier() {
        return pipelineIdentifier;
    }

    public void setPipelineIdentifier(String pipelineIdentifier) {
        this.pipelineIdentifier = pipelineIdentifier;
    }
}
