package es.karmadev.locklogin.common.api.runtime;

import es.karmadev.api.core.KarmaAPI;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.web.WebDownloader;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.plugin.runtime.DependencyManager;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.plugin.runtime.dependency.shade.Relocation;
import es.karmadev.locklogin.api.plugin.runtime.dependency.shade.RelocationSet;
import me.lucko.jarrelocator.JarRelocator;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
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
                    plugin.info("Dependency {0} is being downloaded...", dependency.name());

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

                RelocationSet relocations = dependency.getRelocations();
                if (relocations.hasRelocation()) {
                    Relocation relocation;

                    Path origin = dependency.file().toAbsolutePath();
                    Path destination = origin.getParent().resolve("cache").resolve(PathUtilities.getName(origin, true));

                    if (Files.exists(destination)) {
                        KarmaAPI.inject(destination, CurrentPlugin.getPlugin().plugin().getClass().getClassLoader());
                        dependencies.add(dependency);

                        plugin.info("Loaded dependency {0}", dependency.name());
                        return;
                    }

                    if (!Files.exists(destination.getParent())) {
                        PathUtilities.createDirectory(destination.getParent());
                    }

                    Set<me.lucko.jarrelocator.Relocation> relocationSet = new HashSet<>();
                    while ((relocation = relocations.next()) != null) {
                        relocationSet.add(new me.lucko.jarrelocator.Relocation(relocation.from(), relocation.to()));
                    }

                    JarRelocator relocator = new JarRelocator(origin.toFile(), destination.toFile(), relocationSet);
                    try {
                        relocator.run();

                        KarmaAPI.inject(destination, CurrentPlugin.getPlugin().plugin().getClass().getClassLoader());
                        dependencies.add(dependency);

                        plugin.info("Loaded dependency {0}", dependency.name());
                    } catch (IOException ex2) {
                        plugin.log(ex2, "Failed to relocate {0}", dependency.name());
                        plugin.err("Cannot load dependency {0} because it could not be relocated", dependency.name());
                    }

                    return;
                }

                KarmaAPI.inject(dependency.file(), CurrentPlugin.getPlugin().plugin().getClass().getClassLoader());
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
        KarmaAPI.inject(library, CurrentPlugin.getPlugin().plugin().getClass().getClassLoader());
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
