package es.karmadev.locklogin.api.plugin.file.section;

/**
 * LockLogin movement configuration
 */
public interface MovementConfiguration {

    /**
     * Get if the movement is allowed
     *
     * @return if the movement is allowed
     */
    boolean allow();

    /**
     * Get the movement prevention method
     *
     * @return the movement prevention
     * method
     */
    MovementMethod method();

    /**
     * Get the movement max distance
     *
     * @return the maximum amount of blocks
     * the player can travel if non logged
     */
    int distance();

    /**
     * Movement methods
     */
    enum MovementMethod {
        /**
         * Modify speed to prevent movement
         */
        SPEED,
        /**
         * Teleport the player to prevent movement
         */
        TELEPORT
    }
}
