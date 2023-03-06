package es.karmadev.locklogin.spigot;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;
import es.karmadev.locklogin.api.security.LockLoginHasher;
import es.karmadev.locklogin.common.CPluginNetwork;
import es.karmadev.locklogin.common.dependency.DependencyWrapper;
import es.karmadev.locklogin.common.protection.CPluginHasher;
import es.karmadev.locklogin.common.runtime.CRuntime;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import ml.karmaconfigs.api.common.utils.enums.Level;

import java.lang.reflect.Method;
import java.nio.file.Path;

public class LockLoginSpigot extends KarmaPlugin implements LockLogin {

    private final LockLoginRuntime runtime = new CRuntime();
    private final CPluginNetwork network = new CPluginNetwork();
    private final LockLoginHasher hasher = new CPluginHasher();

    /**
     * Get the LockLogin plugin instance
     *
     * @return the locklogin plugin
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public Object plugin() throws SecurityException {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        return this;
    }

    /**
     * Get the plugin working directory
     *
     * @return the plugin working directory
     */
    @Override
    public Path workingDirectory() {
        return getDataPath();
    }

    /**
     * Get the plugin runtime
     *
     * @return the plugin runtime
     * @throws SecurityException if tried to access from an unauthorized source
     */
    @Override
    public LockLoginRuntime runtime() throws SecurityException {
        runtime.verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);
        return runtime;
    }

    /**
     * Get the plugin network
     *
     * @return the plugin network
     */
    @Override
    public PluginNetwork network() {
        return network;
    }

    /**
     * Get the plugin hasher
     *
     * @return the plugin hasher
     */
    @Override
    public LockLoginHasher hasher() {
        return hasher;
    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void info(final String message, final Object... replaces) {
        console().send(message, Level.INFO, replaces);
    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void warn(final String message, final Object... replaces) {
        console().send(message, Level.WARNING, replaces);
    }

    /**
     * Print a message
     *
     * @param message the message to print
     * @param replaces the message replaces
     */
    @Override
    public void err(final String message, final Object... replaces) {
        console().send(message, Level.GRAVE, replaces);
    }

    /**
     * Enable the KarmaPlugin
     */
    @Override
    public void enable() {
        Class<CurrentPlugin> instance = CurrentPlugin.class;
        try {
            Method initialize = instance.getDeclaredMethod("initialize", LockLogin.class);
            initialize.setAccessible(true);

            initialize.invoke(this);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }

        console().send("Preparing to inject dependencies. Please wait...", Level.WARNING);
        for (LockLoginDependency dependency : DependencyWrapper.dependencies) {
            console().send("Injecting dependency \"{0}\"", Level.INFO, dependency.name());
            runtime.dependencyManager().append(dependency);
        }
    }

    /**
     * Karma source update URL
     *
     * @return the source update URL
     */
    @Override
    public String updateURL() {
        return null;
    }
}
