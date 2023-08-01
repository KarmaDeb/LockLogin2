package es.karmadev.locklogin.api.extension.module.manager;

import es.karmadev.locklogin.api.extension.module.Module;

import java.nio.file.Path;

/**
 * LockLogin module loader
 */
public interface ModuleLoader {

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
     * Try to get a module by its file
     *
     * @param file the module file
     * @return the module
     */
    Module get(final Path file);

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
    Module findByName(final String name);

    /**
     * Find a module
     *
     * @param file the module file
     * @return the module
     */
    Module findByFile(final Path file);

    /**
     * Find a module by a class member
     *
     * @param clazz the class
     * @return the module owning that class
     */
    Module findByClass(final Class<?> clazz);

    /**
     * Find an unloaded module by its name
     *
     * @param name the module name
     * @return the module
     */
    Module findUnloaded(final String name);

    /**
     * Find an unloaded module by its
     * module file
     *
     * @param file the module file
     * @return the module
     */
    Module findUnloaded(final Path file);

    /**
     * Find an unloaded module by a class
     * member
     *
     * @param clazz the class
     * @return the module owning that class
     */
    Module findUnloaded(final Class<?> clazz);

    /**
     * Get all the loaded modules
     *
     * @return the loaded modules
     */
    Module[] getModules();
}
