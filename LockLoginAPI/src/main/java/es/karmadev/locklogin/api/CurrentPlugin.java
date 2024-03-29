package es.karmadev.locklogin.api;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.object.ObjectUtils;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Current LockLogin plugin
 */
@SuppressWarnings({"unused"})
public final class CurrentPlugin {

    @Getter
    private static LockLogin plugin;
    private final static Set<Consumer<LockLogin>> available_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final static Set<Consumer<LockLogin>> sql_available_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Initialization not allowed
     */
    private CurrentPlugin() {}

    /**
     * Initialize the plugin
     *
     * @param instance the plugin instance
     */
    static void initialize(final LockLogin instance) throws SecurityException {
        if (plugin != null) throw new SecurityException("Cannot redefine plugin instance!");
        if (instance.getRuntime() == null) throw new SecurityException();

        plugin = instance;
    }

    /**
     * Update the plugin state
     */
    public static void updateState() {
        if (plugin != null) {
            if (plugin.driver().connected()) {
                sql_available_queue.forEach((c) -> c.accept(plugin));
                sql_available_queue.clear();
            }

            available_queue.forEach((c) -> c.accept(plugin));
            available_queue.clear();
        }
    }

    /**
     * Get the LockLogin plugin as
     * soon as possible
     *
     * @param action the action to perform with the plugin instance
     */
    public static void whenAvailable(final Consumer<LockLogin> action) {
        if (plugin != null && plugin.driver().connected()) {
            action.accept(plugin);
        } else {
            sql_available_queue.add(action);
        }
    }

    /**
     * Get the LockLogin plugin as
     * soon as possible without SQL support
     *
     * @param action the action to perform with the plugin instance
     */
    public static void whenAvailableNoSQL(final Consumer<LockLogin> action) {
        if (plugin != null) {
            action.accept(plugin);
        } else {
            available_queue.add(action);
        }
    }

    /**
     * Get the plugin minimum license version
     *
     * @return the required license version
     */
    public static int licenseVersion() {
        return 3;
    }

    /**
     * Get the plugin language version
     *
     * @return the plugin lang version
     */
    public static int languageVersion() {
        return 1;
    }
}
