package es.karmadev.locklogin.api.plugin.runtime;

import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * LockLogin plugin runtime
 */
public abstract class LockLoginRuntime {

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
