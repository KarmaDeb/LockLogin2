package es.karmadev.locklogin.api.user.auth;

import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;

import java.util.Optional;

/**
 * Auth process factory
 */
public interface ProcessFactory {

    /**
     * Get the next process for the client
     *
     * @param client the client
     * @return the client next process
     */
    Optional<? extends UserAuthProcess> getNextProcess(final NetworkClient client);

    /**
     * Register an auth process
     *
     * @param process the process
     * @throws IllegalStateException if the {@link UserAuthProcess instance} doesn't have a
     * static createFor({@link NetworkClient}) method
     */
    void registerAuthProcess(final Class<? extends UserAuthProcess> process) throws IllegalStateException;
}
