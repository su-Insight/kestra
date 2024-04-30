package io.kestra.core.models.tasks;

import io.kestra.core.models.Plugin;
import io.kestra.core.models.WorkerJobLifecycle;
import io.kestra.core.runners.RunContext;

/**
 * Interface for tasks that are run in the Worker.
 */
public interface RunnableTask <T extends Output> extends Plugin, WorkerJobLifecycle {
    /**
     * This method is called inside the Worker to run (execute) the task.
     * <p>
     * When this task is stopped this method is expected to unblock, quickly finish up any remaining processing, and return.
     */
    T run(RunContext runContext) throws Exception;
}
