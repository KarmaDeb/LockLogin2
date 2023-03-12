package es.karmadev.locklogin.api;

import lombok.Getter;

/**
 * Plugin build type
 */
public enum BuildType {
    /**
     * Release channel
     */
    RELEASE,
    /**
     * Beta channel
     */
    BETA,
    /**
     * Snapshot channel
     */
    SNAPSHOT;

    /**
     * Update id
     */
    @Getter
    private String id;

    /**
     * Update name
     */
    @Getter
    private String name;

    /**
     * Map the data
     *
     * @param id the build id
     * @param name the build name
     * @return
     */
    public BuildType map(final String id, final String name) {
        this.id = id;
        this.name = name;

        return this;
    }
}
