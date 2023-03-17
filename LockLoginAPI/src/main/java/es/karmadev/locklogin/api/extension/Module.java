package es.karmadev.locklogin.api.extension;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.manager.ModuleManager;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import es.karmadev.locklogin.api.plugin.permission.DummyPermission;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import lombok.Getter;
import lombok.experimental.Accessors;
import ml.karmaconfigs.api.common.collection.list.ConcurrentList;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * LockLogin module
 */
@SuppressWarnings("unused")
public abstract class Module implements KarmaSource {

    /**
     * All the loaded module names
     */
    private final static List<String> loaded = new ConcurrentList<>();

    /**
     * Default plugin
     */
    protected final LockLogin plugin = CurrentPlugin.getPlugin();

    /**
     * Default module manager
     */
    protected final ModuleManager manager = plugin.moduleManager();

    /**
     * Module id
     */
    @Getter
    @Accessors(fluent = true)
    private UUID id;

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
        File modFile = getSourceFile();

        try (JarFile jar = new JarFile(modFile)) {
            JarEntry entry = jar.getJarEntry("module.yml");
            if (entry == null) plugin.logErr("Cannot load module from file {0} (nonexistent module.yml)", FileUtilities.getPrettyFile(modFile));

            try (InputStream stream = jar.getInputStream(entry)) {
                KarmaYamlManager module_yml = new KarmaYamlManager(stream);

                if (module_yml.isSet("name")) {
                    name = module_yml.getString("name");
                } else {
                    throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(modFile) + ". Invalid or nonexistent module name");
                }

                if (module_yml.isSet("version")) {
                    version = module_yml.getString("version");
                } else {
                    throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(modFile) + ". Invalid or nonexistent module version");
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
                    throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(modFile) + ". Invalid or nonexistent module description");
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
                    throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(modFile) + ". Invalid or nonexistent module authors");
                }

                @SuppressWarnings("all")
                List<PermissionObject> foundPermissions = new ArrayList<>();
                if (module_yml.isSet("permissions")) {
                    KarmaYamlManager permissionSection = module_yml.getSection("permissions");
                    Set<String> keys = permissionSection.getKeySet();

                    for (String permission : keys) {
                        if (permissionSection.isSection(permission)) {
                            KarmaYamlManager permissionData = permissionSection.getSection(permission);
                            boolean inherits = permissionData.getBoolean("inheritance", false);

                            PermissionObject parentObject = DummyPermission.of(permission, inherits);
                            LockLoginPermission.register(parentObject);

                            if (permissionData.isSet("children")) {
                                KarmaYamlManager childSection = permissionData.getSection("children");
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
    Module(final String name, final String version, final String description, final String[] authors, final PermissionObject[] permissions) throws ExceptionInInitializerError {
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
    public final String name() {
        return name;
    }

    /**
     * Karma source version
     *
     * @return the source version
     */
    @Override
    public final String version() {
        return version;
    }

    /**
     * Karma source description
     *
     * @return the source description
     */
    @Override
    public final String description() {
        return description;
    }

    /**
     * Karma source authors
     *
     * @return the source authors
     */
    @Override
    public final String[] authors() {
        return authors;
    }

    /**
     * Initialize the module
     *
     * @param file the module file
     * @param module_yml the module yml
     * @param initializer the module class initializer
     * @throws Exception if something goes wrong
     */
    static Module initialize(final File file, final KarmaYamlManager module_yml, final Class<? extends Module> initializer) throws Exception {
        String name;
        String version;
        String description;
        String[] authors;
        PermissionObject[] permissions;

        if (module_yml.isSet("name")) {
            name = module_yml.getString("name");
        } else {
            throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or nonexistent module name");
        }

        if (module_yml.isSet("version")) {
            version = module_yml.getString("version");
        } else {
            throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or nonexistent module version");
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
            throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or nonexistent module description");
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
            throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or nonexistent module authors");
        }

        @SuppressWarnings("all")
        List<PermissionObject> foundPermissions = new ArrayList<>();
        if (module_yml.isSet("permissions")) {
            KarmaYamlManager permissionSection = module_yml.getSection("permissions");
            Set<String> keys = permissionSection.getKeySet();

            for (String permission : keys) {
                if (permissionSection.isSection(permission)) {
                    KarmaYamlManager permissionData = permissionSection.getSection(permission);
                    boolean inherits = permissionData.getBoolean("inheritance", false);

                    PermissionObject parentObject = DummyPermission.of(permission, inherits);
                    LockLoginPermission.register(parentObject);

                    if (permissionData.isSet("children")) {
                        KarmaYamlManager childSection = permissionData.getSection("children");
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

    private static void mapChildren(final KarmaYamlManager section, final PermissionObject... topLevels) {
        for (String key : section.getKeySet()) {
            if (section.isSection(key)) {
                KarmaYamlManager permissionData = section.getSection(key);
                boolean inherits = permissionData.getBoolean("inheritance", false);

                PermissionObject parentObject = DummyPermission.of(key, inherits);
                LockLoginPermission.register(parentObject);

                PermissionObject[] clone = Arrays.copyOf(topLevels, topLevels.length + 1);
                clone[clone.length - 1] = parentObject;

                if (permissionData.isSet("children")) {
                    KarmaYamlManager childSection = permissionData.getSection("children");
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
