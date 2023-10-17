package es.karmadev.locklogin.common.api.runtime;

import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CRuntime extends LockLoginRuntime {

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
     */
    @Override
    public Path file()  {
        String path = LockLoginRuntime.class.getProtectionDomain().getCodeSource().getLocation().getFile().replaceAll("%20", " ");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return Paths.get(path);
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
