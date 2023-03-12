package es.karmadev.locklogin.api.network.client;

/**
 * Connection types
 */
public enum ConnectionType {
    /**
     * Online mode
     */
    ONLINE(0),
    /**
     * Offline mode
     */
    OFFLINE(1);

    private final int id;

    /**
     * Initialize the connection type
     *
     * @param id the connection id
     */
    ConnectionType(final int id) {
        this.id = id;
    }

    /**
     * Get the connection id
     *
     * @return the connection id
     */
    public int id() {
        return id;
    }

    /**
     * Get the connection type by its id
     *
     * @param id the connection id type
     * @return the connection type
     */
    public static ConnectionType byId(final int id) {
        if (id == 0) return ConnectionType.ONLINE;
        return ConnectionType.OFFLINE;
    }
}
