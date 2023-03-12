package es.karmadev.locklogin.api.plugin.runtime;

import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;

import java.nio.file.Path;

/**
 * LockLogin dependency manager
 */
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
