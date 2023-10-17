package es.karmadev.locklogin.api.plugin.runtime;

import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * LockLogin plugin runtime
 * @deprecated no longer supported, unfortunately, there are
 * a lot of inconsistencies with the runtime. Even though this
 * will still exist in the plugin, won't have impact anymore
 */
@Deprecated
public abstract class LockLoginRuntime {

    /**
     * Accessible only by the plugin
     */
    public static final int PLUGIN_ONLY = 100;

    /**
     * Accessible only by the plugin and its modules
     */
    public static final int PLUGIN_AND_MODULES = 98;

    /**
     * Initialize the LockLogin runtime
     */
    public LockLoginRuntime() {}

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
     * @deprecated this method won't show any additional information from
     * where the method was called, {@link #verifyIntegrity(int, Class, String)} should
     * be used instead, (ex:
     *  verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES, LockLoginRuntime.class, "#verifyIntegrity(int, Class, String)")
     * )
     */
    @Deprecated
    public void verifyIntegrity(final int permission) {
        verifyIntegrity(permission, LockLoginRuntime.class, "#verifyIntegrity(Class)");
    }

    /**
     * Verify the runtime integrity
     *
     * @param permission the minimum permission authorization level
     * @param clazz the clazz that is verifying integrity
     * @param method the method that is verifying integrity
     */
    public abstract void verifyIntegrity(final int permission, final Class<?> clazz, final String method);

    /**
     * Get if the runtime is completely booted. Meaning
     * the plugin is ready to handle everything
     *
     * @return the plugin boot status
     */
    public abstract boolean booting();

    /**
     * Get the runtime platform
     *
     * @return the runtime platform
     */
    public final Platform platform() {
        try {
            Class<?> proxyServer = Class.forName("net.md_5.bungee.api.ProxyServer");
            Method getInstance = proxyServer.getDeclaredMethod("getInstance");

            Object proxyInstance = getInstance.invoke(proxyServer);
            Class<?> instanceClass = proxyInstance.getClass();

            Method getVersion = instanceClass.getDeclaredMethod("getVersion");
            String version = (String) getVersion.invoke(proxyInstance);

            return Platform.BUNGEE.version(version);
        } catch (Throwable ex) {
            return Platform.BUKKIT.version("UNKNOWN");
        }
    }
}
