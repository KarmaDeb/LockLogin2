package es.karmadev.locklogin.api.user.auth;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;

import java.util.Optional;

/**
 * Auth process factory
 */
public interface ProcessFactory {

    /**
     * Reset a client process
     *
     * @param client the client to reset a process for
     */
    void reset(final NetworkClient client);

    /**
     * Get the next process for the client
     *
     * @param client the client
     * @return the client next process
     */
    Optional<UserAuthProcess> nextProcess(final NetworkClient client);

    /**
     * Get the previous process for the client
     *
     * @param client the client
     * @return the previous process
     */
    Optional<UserAuthProcess> previousProcess(final NetworkClient client);

    /**
     * Register an auth process
     *
     * @param process the process
     * @throws IllegalStateException if the {@link UserAuthProcess instance} doesn't have a
     * static createFor({@link NetworkClient}) method
     */
    void register(final Class<? extends UserAuthProcess> process) throws IllegalStateException;

    /**
     * Get if the process is enabled
     *
     * @param process if the process is enabled
     * @return the process status
     */
    boolean isEnabled(final Class<? extends UserAuthProcess> process);

    /**
     * Get the process name
     *
     * @param process the process
     * @return the process name
     */
    String getName(final Class<? extends UserAuthProcess> process);

    /**
     * Get the process priority
     *
     * @param process the process
     * @return the process priority
     */
    int getPriority(final Class<? extends UserAuthProcess> process);
}
