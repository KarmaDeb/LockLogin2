package es.karmadev.locklogin.api.plugin.file.section;

import es.karmadev.locklogin.api.BuildType;

/**
 * LockLogin updater configuration section
 */
public interface UpdaterSection {

    /**
     * Get the updater build type
     *
     * @return the build type
     */
    BuildType type();

    /**
     * Get if the updater performs checks
     * periodically
     *
     * @return if the updater checks for
     * new versions
     */
    boolean check();

    /**
     * Get the updater check interval
     *
     * @return the check interval
     */
    int interval();
}
