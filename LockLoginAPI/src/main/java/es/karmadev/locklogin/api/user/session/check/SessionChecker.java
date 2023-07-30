package es.karmadev.locklogin.api.user.session.check;

/**
 * Session checker
 */
public interface SessionChecker extends Runnable {

    /**
     * Get if the session checker is
     * running
     *
     * @return if the checker is running
     */
    boolean isRunning();

    /**
     * Restart the session check
     */
    void restart();

    /**
     * Cancel the session check
     */
    void cancel();
}
