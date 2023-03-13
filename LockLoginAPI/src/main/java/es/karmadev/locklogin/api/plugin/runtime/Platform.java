package es.karmadev.locklogin.api.plugin.runtime;

/**
 * Current platform
 */
public enum Platform {
    /**
     * Bukkit server
     */
    BUKKIT,
    /**
     * Bungee server
     */
    BUNGEE;

    private String version;

    /**
     * Update the platform version
     *
     * @param v the version
     * @return this platform
     */
    Platform version(final String v) {
        version = v;
        return this;
    }

    /**
     * Get the platform version
     *
     * @return the platform version
     */
    public String getVersion() {
        return version;
    }
}
