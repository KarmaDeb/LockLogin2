package es.karmadev.locklogin.api.user.auth.process;

import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * User authentication process
 */
public interface UserAuthProcess {

    /**
     * Get the process name
     *
     * @return the process name
     */
    String name();

    /**
     * Get the process priority
     *
     * @return the process priority
     */
    int priority();

    /**
     * Get the process auth type
     *
     * @return the process auth type
     */
    AuthType getAuthType();

    /**
     * Process the auth process
     *
     * @param previous the previous auth process
     * @return the auth process
     */
    CompletableFuture<AuthProcess> process(final AuthProcess previous);
}
