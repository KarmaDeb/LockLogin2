package es.karmadev.locklogin.api;

import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Current LockLogin plugin
 */
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
     * @param instace the plugin instace
     */
    protected static void initialize(final LockLogin instace) throws SecurityException {
        if (plugin != null) throw new SecurityException("Cannot redefine plugin instance!");
        if (instace.runtime() == null) throw new SecurityException();

        instace.runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_ONLY);
        plugin = instace;
    }

    /**
     * Get the LockLogin plugin as
     * soon as possible
     *
     * @param action the action to perform with the plugin instance
     */
    public static void whenAvailable(final Consumer<LockLogin> action) {
        if (plugin != null) {
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
