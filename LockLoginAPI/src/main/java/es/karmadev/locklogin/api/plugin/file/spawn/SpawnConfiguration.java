package es.karmadev.locklogin.api.plugin.file.spawn;

/**
 * Represents the spawn location
 * configuration
 */
@SuppressWarnings("unused")
public interface SpawnConfiguration {

    /**
     * Reload the configuration
     *
     * @return if the configuration
     * was able to be reloaded
     */
    boolean reload();

    /**
     * Get if the spawn is enabled
     *
     * @return if the spawn is enabled
     */
    boolean enabled();

    /**
     * Get if the spawn teleport command
     * is enabled
     *
     * @return if the /spawn command with
     * no argument is enabled
     */
    boolean teleport();

    /**
     * Get the delay before sending a client
     * to the spawn location
     *
     * @return the spawn teleport delay
     */
    int teleportDelay();

    /**
     * Get if the policy is one of the
     * teleport-denying ones
     *
     * @param policy the policy
     * @return if the policy cancels teleport
     */
    boolean cancelWithPolicy(final CancelPolicy policy);

    /**
     * Get if the plugin takes back the
     * client after a successful login
     *
     * @return if the client gets
     * teleported back
     */
    boolean takeBack();

    /**
     * Get the minimum radius the client must
     * be away from spawn in order to store
     * his last location
     *
     * @return the last location store radius
     */
    int spawnRadius();
}
