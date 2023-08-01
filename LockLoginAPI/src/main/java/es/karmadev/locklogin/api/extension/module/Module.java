package es.karmadev.locklogin.api.extension.module;

import es.karmadev.api.core.CoreModule;
import es.karmadev.api.core.DefaultRuntime;
import es.karmadev.api.core.ExceptionCollector;
import es.karmadev.api.core.source.APISource;
import es.karmadev.api.core.source.KarmaSource;
import es.karmadev.api.core.source.runtime.SourceRuntime;
import es.karmadev.api.file.util.NamedStream;
import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.file.util.StreamUtils;
import es.karmadev.api.file.yaml.YamlFileHandler;
import es.karmadev.api.file.yaml.handler.YamlHandler;
import es.karmadev.api.logger.LogManager;
import es.karmadev.api.logger.SourceLogger;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.schedule.task.TaskScheduler;
import es.karmadev.api.strings.StringFilter;
import es.karmadev.api.strings.placeholder.PlaceholderEngine;
import es.karmadev.api.version.Version;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.module.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.module.manager.ModuleManager;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.plugin.permission.DummyPermission;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * LockLogin module
 */
@SuppressWarnings("unused")
public abstract class Module implements APISource {

    /**
     * All the loaded module names
     */
    private final static Set<String> loaded = ConcurrentHashMap.newKeySet();

    /**
     * Default plugin
     */
    protected final LockLogin plugin = CurrentPlugin.getPlugin();

    /**
     * Default module manager
     */
    protected ModuleManager manager = (CurrentPlugin.getPlugin() != null ? plugin.moduleManager() : null);

    /**
     * Module id
     */
    @Getter
    @Accessors(fluent = true)
    private UUID id;

    private final SourceRuntime runtime = new DefaultRuntime(this);

    /**
     * Module name
     */
    private final String name;

    /**
     * Module version
     */
    private final String version;

    /**
     * Module description
     */
    private final String description;

    /**
     * Module authors
     */
    private final String[] authors;

    /**
     * Module permissions
     */
    private final PermissionObject[] permissions;

    /**
     * Create a new module
     *
     * @throws IllegalStateException if any of the module information is invalid
     * @throws ExceptionInInitializerError if the module is already loaded
     */
    public Module() throws IllegalStateException, ExceptionInInitializerError {
        Path modFile = runtime.getFile();

        try (JarFile jar = new JarFile(modFile.toFile())) {
            JarEntry entry = jar.getJarEntry("module.yml");
            if (entry == null) plugin.logErr("Cannot load module from file {0} (nonexistent module.yml)", PathUtilities.pathString(modFile));

            try (InputStream stream = jar.getInputStream(entry)) {
                YamlFileHandler module_yml = YamlHandler.load(stream);
                //KarmaYamlManager module_yml = new KarmaYamlManager(stream);

                if (module_yml.isSet("name")) {
                    name = module_yml.getString("name");
                } else {
                    throw new IllegalStateException("Cannot load module " + PathUtilities.pathString(modFile) + ". Invalid or nonexistent module name");
                }

                if (module_yml.isSet("version")) {
                    version = module_yml.getString("version");
                } else {
                    throw new IllegalStateException("Cannot load module " + PathUtilities.pathString(modFile) + ". Invalid or nonexistent module version");
                }

                if (module_yml.isSet("description")) {
                    String tmpDescription = "";
                    Object value = module_yml.get("description");
                    if (value instanceof String) {
                        tmpDescription = (String) value;
                    }

                    if (value instanceof List) {
                        List<?> unknownList = (List<?>) value;
                        StringBuilder builder = new StringBuilder();
                        int index = 0;
                        for (Object object : unknownList) {
                            builder.append(object).append((index++ != unknownList.size() - 1 ? " " : ""));
                        }

                        tmpDescription = builder.toString();
                    }

                    description = tmpDescription;
                } else {
                    throw new IllegalStateException("Cannot load module " + PathUtilities.pathString(modFile) + ". Invalid or nonexistent module description");
                }

                if (module_yml.isSet("authors")) {
                    String[] tmpAuthors = new String[]{};
                    Object value = module_yml.get("authors");
                    if (value instanceof String) {
                        tmpAuthors = new String[]{ (String) value };
                    }

                    if (value instanceof List) {
                        List<?> unknownList = (List<?>) value;
                        List<String> authorList = new ArrayList<>();
                        for (Object object : unknownList) {
                            authorList.add(String.valueOf(object));
                        }

                        tmpAuthors = authorList.toArray(new String[0]);
                    }

                    authors = tmpAuthors;
                } else {
                    throw new IllegalStateException("Cannot load module " + PathUtilities.pathString(modFile) + ". Invalid or nonexistent module authors");
                }

                @SuppressWarnings("all")
                List<PermissionObject> foundPermissions = new ArrayList<>();
                if (module_yml.isSet("permissions")) {
                    YamlFileHandler permissionSection = module_yml.getSection("permissions");
                    //KarmaYamlManager permissionSection = module_yml.getSection("permissions");
                    Collection<String> keys = permissionSection.getKeys(false);

                    for (String permission : keys) {
                        if (permissionSection.isSection(permission)) {
                            YamlFileHandler permissionData = permissionSection.getSection(permission);
                            boolean inherits = permissionData.getBoolean("inheritance", false);

                            PermissionObject parentObject = DummyPermission.of(permission, inherits);
                            LockLoginPermission.register(parentObject);

                            if (permissionData.isSet("children")) {
                                YamlFileHandler childSection = permissionData.getSection("children");
                                mapChildren(childSection, parentObject);
                            }
                        } else {
                            boolean inherits = permissionSection.getBoolean(permission, false);
                            PermissionObject object = DummyPermission.of(permission, inherits);

                            foundPermissions.add(object);

                            LockLoginPermission.register(object);
                        }
                    }
                }

                permissions = foundPermissions.toArray(new PermissionObject[0]);
                if (loaded.contains(name)) throw new ExceptionInInitializerError("Module " + name + " already initialized!");

                loaded.add(name);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        id = UUID.nameUUIDFromBytes(("LockLoginMod:" + this.getClass().getName()).getBytes());
    }

    /**
     * Creates a new module
     *
     * @param name the module name
     * @param version the module version
     * @param description the module description
     * @param authors the module authors
     * @param permissions the module permissions
     * @throws ExceptionInInitializerError if the module is already loaded
     */
    protected Module(final String name, final String version, final String description, final String[] authors, final PermissionObject[] permissions) throws ExceptionInInitializerError {
        //if (CurrentPlugin.getPlugin() != null) CurrentPlugin.getPlugin().getRuntime().verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY, Module.class, "Module(String, String, String, String[], PermissionObject[])");
        if (loaded.contains(name)) throw new ExceptionInInitializerError("Module " + name + " already initialized!");

        this.name = name;
        this.version = version;
        this.description = description;
        this.authors = authors;
        this.permissions = permissions;

        id = UUID.nameUUIDFromBytes(("LockLoginMod:" + this.getClass().getName()).getBytes());
    }

    /**
     * When the module gets loaded
     */
    public abstract void onLoad();

    /**
     * When the module gets disabled
     */
    public abstract void onUnload();

    /**
     * Get all the permissions registered by the
     * module
     *
     * @return the module permissions
     */
    public final PermissionObject[] getPermissions() {
        return permissions.clone();
    }

    /**
     * Get if the module is loaded
     *
     * @return if the module is loaded
     */
    public final boolean isLoaded() {
        return Arrays.asList(plugin.moduleManager().loader().getModules()).contains(this);
    }

    /**
     * Get the module command
     *
     * @param name the command name
     * @return the module command
     */
    public final ModuleCommand getCommand(final String name) {
        return plugin.moduleManager().commands().getCommand(this.name + ":" + name);
    }

    /**
     * Karma source name
     *
     * @return the source name
     */
    @Override
    public final @NotNull String sourceName() {
        return name;
    }

    /**
     * Karma source version
     *
     * @return the source version
     */
    @Override
    public final @NotNull Version sourceVersion() {
        return Version.parse(version);
    }

    /**
     * Karma source description
     *
     * @return the source description
     */
    @Override
    public final @NotNull String sourceDescription() {
        return description;
    }

    /**
     * Karma source authors
     *
     * @return the source authors
     */
    @Override
    public final String[] sourceAuthors() {
        return authors;
    }

    @Override
    public NamedStream[] findResources() {
        return APISource.super.findResources();
    }

    @Override
    public void loadIdentifier() {
        APISource.super.loadIdentifier();
    }

    @Override
    public void saveIdentifier() {
        APISource.super.saveIdentifier();
    }

    @Override
    public @NotNull String identifier() {
        return name;
    }

    @Override
    public @NotNull SourceRuntime runtime() {
        return runtime;
    }

    @Override
    public @NotNull PlaceholderEngine placeholderEngine(final String s) {
        return ((APISource) CurrentPlugin.getPlugin().plugin()).placeholderEngine(s);
    }

    @Override
    public @NotNull TaskScheduler scheduler(final String s) {
        return ((APISource) CurrentPlugin.getPlugin().plugin()).scheduler(s);
    }

    @Override
    public @NotNull Path workingDirectory() {
        return ((APISource) CurrentPlugin.getPlugin().plugin()).workingDirectory().resolve("mods").resolve(name);
    }

    @Override
    public @NotNull Path navigate(String s, String... strings) {
        Path initial = workingDirectory();
        for (String str : strings) initial = initial.resolve(str);

        return initial.resolve(s);
    }

    @Override
    public @Nullable NamedStream findResource(final String s) {
        JarFile jarHandle = null;
        NamedStream stream = null;
        try {
            Class<? extends Module> clazz = getClass();
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
            Class<? extends Module> clazz = getClass();
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
    public @Nullable CoreModule getModule(String s) {
        return null;
    }

    @Override
    public boolean registerModule(CoreModule coreModule) {
        return false;
    }

    @Override
    public void loadIdentifier(String s) {

    }

    @Override
    public void saveIdentifier(String s) {

    }

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

    /**
     * Initialize the module
     *
     * @param file the module file
     * @param module_yml the module yml
     * @param initializer the module class initializer
     * @throws Exception if something goes wrong
     */
    static Module initialize(final File file, final YamlFileHandler module_yml, final Class<? extends Module> initializer) throws Exception {
        Path path = file.toPath();
        String name;
        String version;
        String description;
        String[] authors;
        PermissionObject[] permissions;

        if (module_yml.isSet("name")) {
            name = module_yml.getString("name");
        } else {
            throw new IllegalStateException("Cannot load module " + PathUtilities.pathString(path) + ". Invalid or nonexistent module name");
        }

        if (module_yml.isSet("version")) {
            version = module_yml.getString("version");
        } else {
            throw new IllegalStateException("Cannot load module " + PathUtilities.pathString(path) + ". Invalid or nonexistent module version");
        }

        if (module_yml.isSet("description")) {
            String tmpDescription = "";
            Object value = module_yml.get("description");
            if (value instanceof String) {
                tmpDescription = (String) value;
            }

            if (value instanceof List) {
                List<?> unknownList = (List<?>) value;
                StringBuilder builder = new StringBuilder();
                int index = 0;
                for (Object object : unknownList) {
                    builder.append(object).append((index++ != unknownList.size() - 1 ? " " : ""));
                }

                tmpDescription = builder.toString();
            }

            description = tmpDescription;
        } else {
            throw new IllegalStateException("Cannot load module " + PathUtilities.pathString(path) + ". Invalid or nonexistent module description");
        }

        if (module_yml.isSet("authors")) {
            String[] tmpAuthors = new String[]{};
            Object value = module_yml.get("authors");
            if (value instanceof String) {
                tmpAuthors = new String[]{ (String) value };
            }

            if (value instanceof List) {
                List<?> unknownList = (List<?>) value;
                List<String> authorList = new ArrayList<>();
                for (Object object : unknownList) {
                    authorList.add(String.valueOf(object));
                }

                tmpAuthors = authorList.toArray(new String[0]);
            }

            authors = tmpAuthors;
        } else {
            throw new IllegalStateException("Cannot load module " + PathUtilities.pathString(path) + ". Invalid or nonexistent module authors");
        }

        @SuppressWarnings("all")
        List<PermissionObject> foundPermissions = new ArrayList<>();
        if (module_yml.isSet("permissions")) {
            YamlFileHandler permissionSection = module_yml.getSection("permissions");
            Collection<String> keys = permissionSection.getKeys(false);

            for (String permission : keys) {
                if (permissionSection.isSection(permission)) {
                    YamlFileHandler permissionData = permissionSection.getSection(permission);
                    boolean inherits = permissionData.getBoolean("inheritance", false);

                    PermissionObject parentObject = DummyPermission.of(permission, inherits);
                    LockLoginPermission.register(parentObject);

                    if (permissionData.isSet("children")) {
                        YamlFileHandler childSection = permissionData.getSection("children");
                        mapChildren(childSection, parentObject);
                    }
                } else {
                    boolean inherits = permissionSection.getBoolean(permission, false);
                    PermissionObject object = DummyPermission.of(permission, inherits);

                    foundPermissions.add(object);

                    LockLoginPermission.register(object);
                }
            }
        }

        permissions = foundPermissions.toArray(new PermissionObject[0]);

        Constructor<? extends Module> superConstructor = initializer.getConstructor(String.class, String.class, String.class, String[].class, PermissionObject[].class);
        superConstructor.setAccessible(true);

        return superConstructor.newInstance(name, version, description, authors, permissions);
    }

    private static void mapChildren(final YamlFileHandler section, final PermissionObject... topLevels) {
        for (String key : section.getKeys(false)) {
            if (section.isSection(key)) {
                YamlFileHandler permissionData = section.getSection(key);
                boolean inherits = permissionData.getBoolean("inheritance", false);

                PermissionObject parentObject = DummyPermission.of(key, inherits);
                LockLoginPermission.register(parentObject);

                PermissionObject[] clone = Arrays.copyOf(topLevels, topLevels.length + 1);
                clone[clone.length - 1] = parentObject;

                if (permissionData.isSet("children")) {
                    YamlFileHandler childSection = permissionData.getSection("children");
                    mapChildren(childSection, clone);
                }
            } else {
                boolean inherits = section.getBoolean(key, false);
                PermissionObject object = DummyPermission.of(key, inherits);

                for (PermissionObject top : topLevels) {
                    top.addChildren(object.addParent(top));
                }

                LockLoginPermission.register(object);
            }
        }
    }
}