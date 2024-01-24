package es.karmadev.locklogin.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Plugin build type
 */
@Getter @AllArgsConstructor
public enum BuildType {
    /**
     * Release channel
     */
    RELEASE("release", "undefined", 2),
    /**
     * Beta channel
     */
    BETA("beta", "undefined", 1),
    /**
     * Snapshot channel
     */
    SNAPSHOT("snapshot", "undefined", 0);

    /**
     * Update id
     */
    private String id;

    /**
     * Update name
     */
    private String name;

    /**
     * Update level, as higher this value is, the more
     * update types the build type will listen for, for example,
     * if we have a BuildType with level 5, it will listen to all
     * updates for update levels from 0-5, but if we have another
     * build type of level 3, it will only listen for updates
     * from 0-3. The lowest level should always be a release and safe
     * for production version
     */
    private final int level;

    /**
     * Map the data
     *
     * @param id the build id
     * @param name the build name
     * @return the build type
     */
    public BuildType map(final String id, final String name) {
        this.id = id;
        this.name = name;

        return this;
    }
}
