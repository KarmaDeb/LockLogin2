package es.karmadev.locklogin.api.plugin.runtime;

import java.nio.file.Path;

/**
 * LockLogin plugin runtime
 */
public abstract class LockLoginRuntime {

    /**
     * Accessible only by the plugin
     */
    public static final int PLUGIN_ONLY = 100;
    /**
     * Accessible only by the module
     */
    public static final int MODULE_ONLY = 99;
    /**
     * Accessible only by the plugin and its modules
     */
    public static final int PLUGIN_AND_MODULES = 98;
    /**
     * Accessible from any source
     * @deprecated useless
     */
    @Deprecated
    public static final int ANY = 0;

    /**
     * Get the plugin runtime dependency manager
     *
     * @return the dependency manager
     */
    public abstract DependencyManager dependencyManager();

    /**
     * Get the plugin file path
     *
     * @return the plugin file path
     * @throws SecurityException if tried to access from an unauthorized source
     */
    public abstract Path file() throws SecurityException;

    /**
     * Get the current caller
     *
     * @return the caller
     */
    public abstract Path caller();

    /**
     * Verify the runtime integrity
     *
     * @param permission the minimum permission authorization level
     * @throws SecurityException if the integrity fails to check
     */
    public abstract void verifyIntegrity(final int permission) throws SecurityException;

    /**
     * Get the runtime platform
     *
     * @return the runtime platform
     */
    public final Platform platform() {
        try {
            Class.forName("net.md_5.bungee.api.ProxiedPlayer");
            return Platform.BUNGEECORD;
        } catch (Throwable ex) {
            try {
                Class.forName("org.bukkit.entity.Player.Spigot");
                return Platform.SPIGOT;
            } catch (Throwable exc) {
                return Platform.BUKKIT;
            }
        }
    }
}
