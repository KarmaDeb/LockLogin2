package es.karmadev.locklogin.api.extension;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.manager.ModuleManager;
import es.karmadev.locklogin.api.network.client.data.PermissionObject;
import ml.karmaconfigs.api.common.collection.list.ConcurrentList;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
     * Initializes the module
     *
     * @throws IllegalStateException if this module has been
     * already initialized
     */
    public Module() throws IllegalStateException {
        File file = getSourceFile();

        try(JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("module.yml");

            if (entry != null && !entry.isDirectory()) {
                try (InputStream stream = jar.getInputStream(entry)) {
                    if (stream != null) {
                        KarmaYamlManager yaml = new KarmaYamlManager(stream);
                        if (yaml.isSet("name")) {
                            name = yaml.getString("name");
                        } else {
                            throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or inexistent module name");
                        }

                        if (yaml.isSet("version")) {
                            version = yaml.getString("version");
                        } else {
                            throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or inexistent module version");
                        }

                        if (yaml.isSet("description")) {
                            String tmpDescription = "";
                            Object value = yaml.get("description");
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
                            throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or inexistent module description");
                        }

                        if (yaml.isSet("authors")) {
                            String[] tmpAuthors = new String[]{};
                            Object value = yaml.get("authors");
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
                            throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or inexistent module authors");
                        }

                        @SuppressWarnings("all")
                        List<PermissionObject> foundPermissions = new ArrayList<>();
                        if (yaml.isSet("permissions")) {
                            KarmaYamlManager permissionSection = yaml.getSection("permissions");

                        }
                        permissions = foundPermissions.toArray(new PermissionObject[0]);

                        Module loaded = plugin.moduleManager().loader().find(name);
                        if (loaded != null) throw new ExceptionInInitializerError("Cannot initialize class " + this.getClass().getName() + ". Is this module already initialized?");
                    } else {
                        throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or inexistent module.yml");
                    }
                }
            } else {
                throw new IllegalStateException("Cannot load module " + FileUtilities.getPrettyFile(file) + ". Invalid or inexistent module.yml");
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
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
        return plugin.moduleManager().commands().find(this.name + ":" + name);
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
}
