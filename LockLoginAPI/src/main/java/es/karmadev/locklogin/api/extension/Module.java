package es.karmadev.locklogin.api.extension;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.event.handler.EventHandler;
import es.karmadev.locklogin.api.extension.command.CommandRegistrar;
import es.karmadev.locklogin.api.extension.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.manager.ModuleManager;
import ml.karmaconfigs.api.common.collection.list.ConcurrentList;
import ml.karmaconfigs.api.common.data.file.FileUtilities;
import ml.karmaconfigs.api.common.karma.file.yaml.KarmaYamlManager;
import ml.karmaconfigs.api.common.karma.source.KarmaSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * LockLogin module
 */
public abstract class Module implements KarmaSource {

    private final static List<String> loaded = new ConcurrentList<>();

    protected final LockLogin plugin = CurrentPlugin.getPlugin();
    protected final ModuleManager manager = plugin.moduleManager();

    private final String name;
    private final String version;
    private final String description;
    private final String[] authors;

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
                                    builder.append(String.valueOf(object)).append((index++ != unknownList.size() - 1 ? " " : ""));
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

    /**
     * Create a new empty event handler
     *
     * @return the event handler
     */
    public static EventHandler emptyHandler() {
        return new EventHandler() {};
    }
}
