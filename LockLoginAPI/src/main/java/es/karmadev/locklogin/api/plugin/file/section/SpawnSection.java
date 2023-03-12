package es.karmadev.locklogin.api.plugin.file.section;

/**
 * LockLogin spawn section configuration
 */
public interface SpawnSection {

    /**
     * Get if the plugin manages the spawn
     *
     * @return if the plugin manages
     * the spawn
     */
    boolean enable();

    /**
     * Get if the plugin takes back the
     * client to his last location
     *
     * @return if the plugin takes
     * the client back
     */
    boolean takeBack();

    /**
     * Get the spawn minimum distance. This
     * will control since how many blocks
     * the location will start to be
     * stored
     *
     * @return the spawn minimum distance
     */
    int minDistance();
}
