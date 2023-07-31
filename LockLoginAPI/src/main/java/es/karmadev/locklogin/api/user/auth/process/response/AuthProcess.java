package es.karmadev.locklogin.api.user.auth.process.response;

import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;

/**
 * Auth process
 */
public interface AuthProcess {

    /**
     * Get if the process was completed
     * successfully
     *
     * @return the process status
     */
    boolean wasSuccess();

    /**
     * Get the user auth process
     *
     * @return the user auth process
     */
    UserAuthProcess authProcess();
}
