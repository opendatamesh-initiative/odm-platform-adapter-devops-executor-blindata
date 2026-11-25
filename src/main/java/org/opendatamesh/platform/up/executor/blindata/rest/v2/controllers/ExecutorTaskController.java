package org.opendatamesh.platform.up.executor.blindata.rest.v2.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.TaskResourceMapper;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.TaskRunCommandRes;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.logs.TaskLogsRes;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.start.TaskStartCommandRes;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.start.TaskStartResultRes;
import org.opendatamesh.platform.up.executor.blindata.rest.v2.resources.tasks.usecases.status.TaskStatusRes;
import org.opendatamesh.platform.up.executor.blindata.tasks.services.ExecutorTaskUseCasesService;

/** DevOps executor API v2 for the "verify contract" task. */
@Path("/api/v2/up/executor/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Executor tasks")
public class ExecutorTaskController {

    private final ExecutorTaskUseCasesService service;
    private final TaskResourceMapper mapper;

    public ExecutorTaskController(ExecutorTaskUseCasesService service, TaskResourceMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @POST
    @Path("/start")
    @Operation(summary = "Start the contract verification of a data product version",
            description = "Reads the version's descriptor from the Registry and runs its executable contract rules on the "
                    + "Blindata agent of the environment, in dry run.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Verification job started"),
            @APIResponse(responseCode = "400", description = "Missing parameters, unknown environment, version not found, "
                    + "or blocking rules the agent cannot run in this environment"),
            @APIResponse(responseCode = "502", description = "The Registry or the Blindata agent failed or refused the credentials")
    })
    public TaskStartResultRes start(TaskStartCommandRes body) {
        return mapper.toStartResult(service.start(mapper.toStartCommand(body)));
    }

    @POST
    @Path("/status")
    @Operation(summary = "Read the status of a verification run",
            description = "When the run has ended the verdict comes from the blocking rules, and the report is posted to "
                    + "DevOps (when configured) before the terminal status is answered.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Run status"),
            @APIResponse(responseCode = "400", description = "Malformed providerRunId or unknown environment"),
            @APIResponse(responseCode = "404", description = "Unknown agent job"),
            @APIResponse(responseCode = "502", description = "The Blindata agent failed or refused the credentials")
    })
    public TaskStatusRes status(TaskRunCommandRes body) {
        return mapper.toStatusRes(service.status(mapper.toStatusCommand(body)));
    }

    @POST
    @Path("/logs")
    @Operation(summary = "Read the verification report and the agent job log as one block")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Report and job log"),
            @APIResponse(responseCode = "400", description = "Malformed providerRunId or unknown environment"),
            @APIResponse(responseCode = "404", description = "Unknown agent job"),
            @APIResponse(responseCode = "502", description = "The Blindata agent failed or refused the credentials")
    })
    public TaskLogsRes logs(TaskRunCommandRes body) {
        return mapper.toLogsRes(service.logs(mapper.toLogsCommand(body)));
    }

    @POST
    @Path("/cancel")
    @Operation(summary = "Cancel a verification run",
            description = "The agent cannot stop an ad-hoc job: a running job is accepted and ends on its own.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Cancel accepted"),
            @APIResponse(responseCode = "400", description = "Malformed providerRunId or unknown environment"),
            @APIResponse(responseCode = "404", description = "Unknown agent job"),
            @APIResponse(responseCode = "409", description = "The job has already ended"),
            @APIResponse(responseCode = "502", description = "The Blindata agent failed or refused the credentials")
    })
    public Response cancel(TaskRunCommandRes body) {
        service.cancel(mapper.toCancelCommand(body));
        return Response.ok().build();
    }
}
