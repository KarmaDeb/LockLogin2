package es.karmadev.locklogin.api.network.communication.data;

/**
 * LockLogin channel
 */
@SuppressWarnings("unused")
public enum Channel {
    /**
     * Plugin channel
     */
    PLUGIN("ll:plugin"),
    /**
     * Player data channel
     */
    PLAYER("ll:player");

    /**
     * The channel name
     */
    public final String name;

    /**
     * Initialize the channel
     *
     * @param name the channel name
     */
    Channel(final String name) {
        this.name = name;
    }

    /**
     * Get a channel from its name
     *
     * @param name the channel name
     * @return the channel
     */
    public static Channel fromName(final String name) {
        String raw = name.toLowerCase();
        switch (raw) {
            case "ll:plugin":
                return PLUGIN;
            case "ll:player":
                return PLAYER;
            default:
                return null;
        }
    }
}
