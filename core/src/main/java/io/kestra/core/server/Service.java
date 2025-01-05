package io.kestra.core.server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Interface for Kestra's Service
 */
public interface Service {

    /**
     * Gets the unique identifier for this service.
     *
     * @return the string id.
     */
    String getId();

    /**
     * Gets the service type.
     *
     * @return the type.
     */
    ServiceType getType();

    /**
     * Gets the service state.
     *
     * @return the state.
     */
    ServiceState getState();

    /**
     * Specify whether to skip graceful termination on shutdown.
     *
     * @param skipGracefulTermination {@code true} to skip graceful termination on shutdown.
     */
    default void skipGracefulTermination(final boolean skipGracefulTermination) {
        // noop
    }

    /**
     * Supported service types.
     */
    enum ServiceType {
        EXECUTOR,
        INDEXER,
        SCHEDULER,
        WEBSERVER,
        WORKER,
    }

    /**
     * {@link ServiceState} are the possible states that a Kestra's Service can be in.
     * An instance must only be in one state at a time.
     * The expected state transition with the following defined states is:
     *
     * <pre>
     *
     *                 +--------------+
     *                 | Created (0)  |------------->+
     *                 +------+-------+              |
     *                        |                      |
     *                        v                      |
     *                 +--------------+              |
     *         +<----- | Running (1)  | ------------>+
     *         |       +------+-------+              |
     *    +----+----+         |                      |
     *    | Error(2)|         |                      |
     *    +----+----+         |                      |
     *         |              v                      v
     *         |       +------+----------+    +------+------------+
     *         +-----&gt; | Terminating (4) |&lt;---| Disconnected (3)  |
     *                 +------+----------+    +-------------------+
     *                   |          |
     *                   v          v
     *      +------+-------+       +------+-------+
     *      | Terminated   |       | Terminated   |
     *      | Graceful (5)  |      | Forced (6)   |
     *      +--------------+       +--------------+
     *                    |         |
     *                    v         v
     *                  +------+-------+
     *                  | Not          |
     *                  | Running (7)  |
     *                  +------+-------+
     *                         |
     *                         v
     *                  +------+-------+
     *                  | Empty (8)    |
     *                  +------+-------+
     * </pre>
     */
    enum ServiceState {
        CREATED(1, 2, 3),               // 0
        RUNNING(2, 3, 4),               // 1
        ERROR(4),                       // 2
        DISCONNECTED(4, 7),             // 3
        TERMINATING(5, 6, 7),           // 4
        TERMINATED_GRACEFULLY(7),       // 5
        TERMINATED_FORCED(7),           // 6
        NOT_RUNNING(8),                 // 7
        EMPTY();                         // 8

        private final Set<Integer> validTransitions = new HashSet<>();

        ServiceState(final Integer... validTransitions) {
            this.validTransitions.addAll(Arrays.asList(validTransitions));
        }

        public boolean isValidTransition(final ServiceState newState) {
            return validTransitions.contains(newState.ordinal()) || equals(newState);
        }

        public boolean isRunning() {
            return equals(CREATED)
                || equals(RUNNING);
        }

        public boolean isDisconnectedOrTerminating() {
            return equals(TERMINATING)
                || equals(DISCONNECTED);
        }

        public boolean hasCompletedTermination() {
            return equals(TERMINATED_GRACEFULLY)
                || equals(TERMINATED_FORCED)
                || equals(NOT_RUNNING)
                || equals(EMPTY);
        }
    }
}
