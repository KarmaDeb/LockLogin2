package es.karmadev.locklogin.test;

import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.common.api.runtime.CDependencyManager;

import java.nio.file.Path;
import java.nio.file.Paths;

public class AllowAllRuntime extends LockLoginRuntime {

    private final DependencyManager manager = new CDependencyManager();
    public boolean booted = false;

    /**
     * Get the plugin runtime dependency manager
     *
     * @return the dependency manager
     */
    @Override
    public DependencyManager dependencyManager() {

        return manager;
    }

    /**
     * Get the plugin file path
     *
     * @return the plugin file path
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public Path file() throws SecurityException {
        return Paths.get("");
    }

    /**
     * Get the current caller
     *
     * @return the caller
     */
    @Override
    public Path caller() {
        return file();
    }

    /**
     * Verify the runtime integrity
     *
     * @param permission the minimum permission authorization level
     * @param targetClazz      the clazz that is verifying integrity
     * @param targetMethod     the method that is verifying integrity
     * @throws SecurityException if the integrity fails to check
     */
    @Override
    public void verifyIntegrity(final int permission, final Class<?> targetClazz, String targetMethod) throws SecurityException {
        //Allow all
    }

    /**
     * Get if the runtime is completely booted. Meaning
     * the plugin is ready to handle everything
     *
     * @return the plugin boot status
     */
    @Override
    public boolean booting() {
        return !booted;
    }
}
