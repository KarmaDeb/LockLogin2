package es.karmadev.locklogin.api.plugin.file.section;

/**
 * Brute force plugin configuration
 */
public interface BruteForceSection {

    /**
     * Get the maximum amount of login
     * attempts
     *
     * @return the login attempts
     */
    int attempts();

    /**
     * Get the maximum amount of
     * tries
     *
     * @return the maximum amount of
     * login attempt tries
     */
    int tries();

    /**
     * Get the connection block time
     * after the maximum amount of
     * tries is reached
     *
     * @return the connection block
     * time
     */
    int blockTime();
}
