package es.karmadev.locklogin.common.api.runtime;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import ml.karmaconfigs.api.common.ResourceDownloader;
import ml.karmaconfigs.api.common.karma.loader.BruteLoader;

import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class CDependencyManager implements DependencyManager {

    private final Set<LockLoginDependency> dependencies = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Append a dependency
     *
     * @param dependency the dependency to append
     */
    @Override
    public void append(final LockLoginDependency dependency) {
        if (dependencies.stream().noneMatch((stored) -> stored.name().equals(dependency.name()))) {
            if (dependency.needsInstallation()) {
                LockLogin plugin = CurrentPlugin.getPlugin();

                DependencyChecksum generated = dependency.generateChecksum();
                DependencyChecksum loaded = dependency.checksum();

                boolean download;
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
}
