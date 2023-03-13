package es.karmadev.locklogin.common.api.extension.loader;

import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.extension.manager.ModuleLoader;

import java.nio.file.Path;

public class CModuleLoader implements ModuleLoader {

    /**
     * Load a module
     *
     * @param module the module to load
     */
    @Override
    public void load(final Module module) {

    }

    /**
     * Try to load a module from a file
     *
     * @param file the module file
     * @return the loaded module
     * @throws IllegalStateException if the module is not in a valid
     *                               directory or is not a valid module
     */
    @Override
    public Module load(final Path file) throws IllegalStateException {
        return null;
    }

    /**
     * Unload a module
     *
     * @param module the module to unload
     */
    @Override
    public void unload(final Module module) {

    }

    /**
     * Find a module
     *
     * @param name the module name
     * @return the module
     */
    @Override
    public Module find(final String name) {
        return null;
    }

    /**
     * Find a module
     *
     * @param file the module file
     * @return the module
     */
    @Override
    public Module find(final Path file) {
        return null;
    }

    /**
     * Get all the loaded modules
     *
     * @return the loaded modules
     */
    @Override
    public Module[] getModules() {
        return new Module[0];
    }
}
