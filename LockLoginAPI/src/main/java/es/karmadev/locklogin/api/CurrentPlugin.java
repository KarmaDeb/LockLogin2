package es.karmadev.locklogin.api;

import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Current LockLogin plugin
 */
@SuppressWarnings("unused")
public final class CurrentPlugin {

    private static LockLogin plugin;
    private final static Set<Consumer<LockLogin>> available_queue = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        if (instance.runtime() == null) throw new SecurityException();

        instance.runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY);
        plugin = instance;
    }

    /**
     * Update the plugin state
     */
    public static void updateState() {
        if (plugin != null && plugin.driver().connected()) {
            available_queue.forEach((c) -> c.accept(plugin));
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
            available_queue.add(action);
        }
    }

    /**
     * Get the LockLogin plugin
     *
     * @return the plugin
     */
    public static LockLogin getPlugin() {
        return plugin;
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
