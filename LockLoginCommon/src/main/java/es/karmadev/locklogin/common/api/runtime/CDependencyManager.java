package es.karmadev.locklogin.common.api.runtime;

import es.karmadev.api.web.WebDownloader;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;

import java.net.URL;
import java.nio.file.Path;
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
            try {
                Class.forName(dependency.testClass());
            } catch (ClassNotFoundException ex) {
                LockLogin plugin = CurrentPlugin.getPlugin();

                DependencyChecksum generated = dependency.generateChecksum();
                DependencyChecksum loaded = dependency.checksum();

                boolean download = generated == null || loaded == null || !generated.matches(loaded);
                if (download) {
                    plugin.info("Dependency {0} is not downloaded. Downloading it...", dependency.name());

                    URL url = dependency.downloadURL();
                    if (url == null) {
                        plugin.err("Cannot download dependency {0} because its download URL is not valid", dependency.name());
                        return;
                    }

                    WebDownloader downloader = new WebDownloader(url);
                    try {
                        downloader.download(dependency.file());
                    } catch (Throwable ex2) {
                        ex2.printStackTrace();
                    }
                }

                //TODO: Use KarmaAPI2's brute loader
                /*BruteLoader loader = new BruteLoader(CurrentPlugin.getPlugin().plugin().getClass().getClassLoader());
                loader.add(dependency.file());*/
                dependencies.add(dependency);

                plugin.info("Loaded dependency {0}", dependency.name());
            }
        }
    }

    /**
     * Append an external library
     *
     * @param library the library
     */
    @Override
    public void appendExternal(final Path library) {
        //TODO: Use KarmaAPI2's brute loader
        /*BruteLoader loader = new BruteLoader(CurrentPlugin.getPlugin().plugin().getClass().getClassLoader());
        loader.add(library);*/
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
