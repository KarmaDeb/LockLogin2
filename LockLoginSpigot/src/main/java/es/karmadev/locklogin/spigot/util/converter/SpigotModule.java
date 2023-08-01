package es.karmadev.locklogin.spigot.util.converter;

import es.karmadev.api.core.CoreModule;
import es.karmadev.api.core.DefaultRuntime;
import es.karmadev.api.core.ExceptionCollector;
import es.karmadev.api.core.source.KarmaSource;
import es.karmadev.api.core.source.runtime.SourceRuntime;
import es.karmadev.api.file.util.NamedStream;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.util.StreamUtils;
import es.karmadev.api.logger.LogManager;
import es.karmadev.api.logger.SourceLogger;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.schedule.task.TaskScheduler;
import es.karmadev.api.strings.StringFilter;
import es.karmadev.api.strings.placeholder.PlaceholderEngine;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.extension.plugin.PluginModule;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.common.api.runtime.CRuntime;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class SpigotModule extends PluginModule<JavaPlugin> {

    private final static LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final SourceRuntime runtime = new DefaultRuntime(this);
    private final Path pluginFile;

    public SpigotModule(final JavaPlugin owner, final Path pluginFile) {
        super(owner,
                owner.getName(),
                owner.getDescription().getVersion(),
                owner.getDescription().getDescription(),
                owner.getDescription().getAuthors().toArray(new String[0]));
        this.pluginFile = pluginFile;
    }

    /**
     * When the module gets loaded
     */
    @Override
    public void onLoad() {
        //We expect the plugin to be already loaded
    }

    /**
     * When the module gets disabled
     */
    @Override
    public void onUnload() {
        //We better don't touch this
    }

    /**
     * Get the module file
     *
     * @return the module file
     */
    @Override
    public Path getFile() {
        return pluginFile;
    }

    @Override
    public @NotNull String identifier() {
        return plugin.getDescription().getMain();
    }

    @Override
    public @Nullable URI sourceUpdateURI() {
        return null;
    }

    @Override
    public @NotNull SourceRuntime runtime() {
        return runtime;
    }

    @Override
    public @NotNull PlaceholderEngine placeholderEngine(final String s) {
        return spigot.plugin().placeholderEngine(s);
    }

    @Override
    public @NotNull TaskScheduler scheduler(final String s) {
        return spigot.plugin().scheduler(s);
    }

    @Override
    public @NotNull Path workingDirectory() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public @NotNull Path navigate(final String s, final String... strings) {
        Path initial = workingDirectory();
        for (String str : strings) {
            initial = initial.resolve(str);
        }

        return initial.resolve(s);
    }

    @Override
    public @Nullable NamedStream findResource(final String s) {
        JarFile jarHandle = null;
        NamedStream stream = null;
        try {
            Class<? extends SpigotModule> clazz = getClass();
            ProtectionDomain domain = clazz.getProtectionDomain();
            if (domain == null) return null;

            CodeSource source = domain.getCodeSource();
            if (source == null) return null;

            URL location = source.getLocation();
            if (location == null) return null;

            String filePath = location.getFile().replaceAll("%20", " ");
            File file = new File(filePath);

            jarHandle = new JarFile(file);

            JarEntry entry = jarHandle.getJarEntry(s);
            if (entry == null || entry.isDirectory()) return null;

            InputStream streamHandle = jarHandle.getInputStream(entry);
            stream = NamedStream.newStream(entry.getName(), StreamUtils.clone(streamHandle, true));
        } catch (IOException ex) {
            ExceptionCollector.catchException(KarmaSource.class, ex);
        } finally {
            if (jarHandle != null) {
                try {
                    jarHandle.close();
                } catch (IOException ignored) {}
            }
        }

        return stream;
    }

    @Override
    public @NotNull NamedStream[] findResources(final String s, @Nullable StringFilter stringFilter) {
        JarFile jarHandle = null;
        List<NamedStream> handles = new ArrayList<>();
        try {
            Class<? extends SpigotModule> clazz = getClass();
            ProtectionDomain domain = clazz.getProtectionDomain();
            if (domain == null) return null;

            CodeSource source = domain.getCodeSource();
            if (source == null) return null;

            URL location = source.getLocation();
            if (location == null) return null;

            String filePath = location.getFile().replaceAll("%20", " ");
            File file = new File(filePath);

            jarHandle = new JarFile(file);
            Enumeration<JarEntry> entries = jarHandle.entries();

            do {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                String name = entry.getName();
                if (stringFilter == null || stringFilter.accept(name)) {
                    try (InputStream stream = jarHandle.getInputStream(entry)) {
                        handles.add(NamedStream.newStream(name, stream));
                    }
                }
            } while (entries.hasMoreElements());
        } catch (IOException ex) {
            ExceptionCollector.catchException(KarmaSource.class, ex);
        } finally {
            if (jarHandle != null) {
                try {
                    jarHandle.close();
                } catch (IOException ignored) {}
            }
        }

        return handles.toArray(new NamedStream[0]);
    }

    @Override
    public boolean export(final String s, final Path path) {
        try (NamedStream single = findResource(s)) {
            if (single != null) {
                return tryExport(single, path);
            }
        } catch (IOException ex) {
            ExceptionCollector.catchException(KarmaSource.class, ex);
        }

        NamedStream[] streams = findResources(s, null);
        if (streams.length == 0) return false; //We export nothing

        int success = 0;
        for (NamedStream stream : streams) {
            try {
                if (tryExport(stream, path)) {
                    success++;
                }
            } finally {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }

        return success == streams.length;
    }

    @Override
    public SourceLogger logger() {
        return LogManager.getLogger(this);
    }

    @Override
    public @Nullable CoreModule getModule(final String s) {
        return null;
    }

    @Override
    public boolean registerModule(final CoreModule coreModule) {
        return false;
    }

    @Override
    public void loadIdentifier(final String s) {}

    @Override
    public void saveIdentifier(final String s) {}

    private boolean tryExport(final NamedStream stream, final Path directory) {
        String name = stream.getName();
        Path targetFile = directory;
        if (name.contains("/")) {
            String[] data = name.split("/");
            for (String dir : data) {
                //Are we the start route?
                if (!ObjectUtils.isNullOrEmpty(dir)) {
                    targetFile = targetFile.resolve(dir);
                }
            }
        } else {
            targetFile = targetFile.resolve(name);
        }

        String raw = StreamUtils.streamToString(stream);
        return PathUtilities.write(targetFile, raw);
    }
}
