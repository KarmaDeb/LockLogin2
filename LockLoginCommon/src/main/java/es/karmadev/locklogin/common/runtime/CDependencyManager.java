package es.karmadev.locklogin.common.runtime;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import ml.karmaconfigs.api.common.ResourceDownloader;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CDependencyManager implements DependencyManager {

    private final Set<LockLoginDependency> dependencies = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Module> modules = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Append a dependency
     *
     * @param dependency the dependency to append
     */
    @Override
    public void append(final LockLoginDependency dependency) {
        if (dependencies.stream().noneMatch((stored) -> stored.name().equals(dependency.name()))) {
            LockLogin plugin = CurrentPlugin.getPlugin();

            DependencyChecksum generated = dependency.generateChecksum();
            DependencyChecksum loaded = dependency.checksum();

            boolean download = false;
            if (generated == null || loaded == null) {
                download = !Files.exists(dependency.file());
            } else {
                download = !generated.matches(loaded);
            }

            if (download) {
                plugin.info("Dependency {0} is not downloaded. Downloading it...", dependency.name());

                URL url = dependency.downloadURL();
                if (url == null) {
                    plugin.err("Cannot download dependency {0} because its download URL is not valid", dependency.name());
                    return;
                }

                ResourceDownloader downloader = new ResourceDownloader(dependency.file(), url);
                downloader.download();
            }

            BruteLoader loader = new BruteLoader(CurrentPlugin.getPlugin().getClass().getClassLoader());
            loader.add(dependency.file());
            dependencies.add(dependency);

            plugin.info("Loaded dependency {0}", dependency.name());
        }
    }

    /**
     * Get all the loaded dependencies
     *
     * @return the loaded dependencies
     */
    @Override
    public LockLoginDependency[] getLoaded() {
        return dependencies.toArray(new LockLoginDependency[0]).clone();
    }

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
