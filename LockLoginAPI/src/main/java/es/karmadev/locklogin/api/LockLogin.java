package es.karmadev.locklogin.api;

import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.api.security.hash.PluginHash;

import java.nio.file.Path;

/**
 * LockLogin plugin
 */
public interface LockLogin {

    /**
     * Get the LockLogin plugin instance
     *
     * @return the locklogin plugin
     * @throws SecurityException if tried to access from an unauthorized source
     */
    Object plugin() throws SecurityException;

    /**
     * Get the plugin working directory
     *
     * @return the plugin working directory
     */
    Path workingDirectory();

    /**
     * Get the plugin runtime
     *
     * @return the plugin runtime
     * @throws SecurityException if tried to access from an unauthorized source
     */
    LockLoginRuntime runtime() throws SecurityException;

    /**
     * Get the plugin network
     *
     * @return the plugin network
     */
    PluginNetwork network();

    /**
     * Get the plugin hasher
     *
     * @return the plugin hasher
     */
    LockLoginHasher hasher();

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    void info(final String message, final Object... replaces);

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    void warn(final String message, final Object... replaces);

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    void err(final String message, final Object... replaces);
}
