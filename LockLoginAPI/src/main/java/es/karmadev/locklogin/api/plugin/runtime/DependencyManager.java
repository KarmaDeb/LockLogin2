package es.karmadev.locklogin.api.plugin.runtime;

import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;

/**
 * LockLogin dependency manager
 */
@SuppressWarnings("unused")
public interface DependencyManager {

    /**
     * Append a dependency
     *
     * @param dependency the dependency to append
     */
    void append(final LockLoginDependency dependency);

    /**
     * Get all the loaded dependencies
     *
     * @return the loaded dependencies
     */
    LockLoginDependency[] getLoaded();
}
