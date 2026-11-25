package org.opendatamesh.platform.up.executor.blindata.clients.agent;

import org.opendatamesh.platform.up.executor.blindata.tasks.model.AgentRun;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.ContractJob;
import org.opendatamesh.platform.up.executor.blindata.tasks.model.EnvironmentProfile;

import java.util.Optional;

/** The Blindata agent of an environment profile: job runner and job history. */
public interface AgentClient {

    /** Starts the job asynchronously and returns its sequence id. Never retried. */
    long launch(EnvironmentProfile profile, ContractJob job);

    /** Empty when the agent has no job with this sequence id. */
    Optional<AgentRun> readRun(EnvironmentProfile profile, long sequenceId);

    /** The agent job log, bounded in size. Empty when the agent has none. */
    Optional<String> readLog(EnvironmentProfile profile, long sequenceId);
}
