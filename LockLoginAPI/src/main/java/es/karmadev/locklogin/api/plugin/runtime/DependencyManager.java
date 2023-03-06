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

    /**
     * Load a module
     *
     * @param module the module to load
     */
    void load(final Module module);

    /**
     * Try to load a module from a file
     *
     * @param file the module file
     * @return the loaded module
     * @throws IllegalStateException if the module is not in a valid
     * directory or is not a valid module
     */
    Module load(final Path file) throws IllegalStateException;

    /**
     * Unload a module
     *
     * @param module the module to unload
     */
    void unload(final Module module);

    /**
     * Find a module
     *
     * @param name the module name
     * @return the module
     */
    Module find(final String name);

    /**
     * Find a module
     *
     * @param file the module file
     * @return the module
     */
    Module find(final Path file);

    /**
     * Get all the loaded modules
     *
     * @return the loaded modules
     */
    Module[] getModules();
}
