package es.karmadev.locklogin.api;

import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

/**
 * Current LockLogin plugin
 */
public final class CurrentPlugin {

    private static LockLogin plugin;

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
     * Get the LockLogin plugin
     *
     * @return the plugin
     */
    public static LockLogin getPlugin() {
        return plugin;
    }
}
