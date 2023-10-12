package es.karmadev.locklogin.api.user.auth.process;

import es.karmadev.locklogin.api.task.FutureTask;
import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;

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
     * Get if the process is enabled
     *
     * @return the process status
     */
    boolean isEnabled();

    /**
     * Set the process status
     *
     * @param status the process status
     */
    void setEnabled(final boolean status);

    /**
     * Process the auth process
     *
     * @param previous the previous auth process
     * @return the auth process
     */
    FutureTask<AuthProcess> process(final AuthProcess previous);
}
