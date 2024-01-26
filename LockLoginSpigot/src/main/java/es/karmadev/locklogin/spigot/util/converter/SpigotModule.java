package es.karmadev.locklogin.spigot.util.converter;

import es.karmadev.locklogin.api.extension.module.*;
import es.karmadev.locklogin.api.extension.module.resource.ResourceHandle;
import es.karmadev.locklogin.api.network.PluginNetwork;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpigotModule implements PluginModule<JavaPlugin> {

    private final JavaPlugin plugin;
    private final Path file;
    private final SPDescription description;
    private final SpigotModuleMaker maker;
    private final PluginNetwork network;
    private final ModuleManager loader;

    private final MethodHandle onModDisable;
    private final MethodHandle onModEnable;

    public SpigotModule(final JavaPlugin plugin, final Path file, final SpigotModuleMaker maker, final PluginNetwork network, final ModuleManager loader) {
        this.plugin = plugin;
        this.file = file;
        this.description = new SPDescription(this);
        this.maker = maker;
        this.network = network;
        this.loader = loader;

        MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodHandle onDisable = null;
        try {
            Method method = plugin.getClass().getDeclaredMethod("onDisabled", Module.class);
            onDisable = lookup.unreflect(method).bindTo(plugin);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            plugin.getLogger().log(Level.WARNING, "Hooked into LockLogin as a plugin without #onDisabled(Module)");
        }
        onModDisable = onDisable;

        MethodHandle onEnable = null;
        try {
            Method method = plugin.getClass().getDeclaredMethod("onEnabled", Module.class);
            onEnable = lookup.unreflect(method).bindTo(plugin);
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            plugin.getLogger().log(Level.WARNING, "Hooked into LockLogin as a plugin without #onEnabled(Module)");
        }
        onModEnable = onEnable;
    }

    /**
     * Get if the module is from the
     * marketplace
     *
     * @return if the module is from the marketplace
     */
    @Override
    public boolean isFromMarketplace() {
        return false; //This can never be a market-resource
    }

    /**
     * Get the module file
     *
     * @return the module file
     */
    @Override
    public @NotNull Path getFile() {
        return file;
    }

    /**
     * Get the module data folder
     *
     * @return the module data folder
     */
    @Override
    public @NotNull Path getDataFolder() {
        return plugin.getDataFolder().toPath();
    }

    /**
     * Get the module logger
     *
     * @return the module logger
     */
    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    /**
     * Get the module description
     *
     * @return the module description
     */
    @Override
    public @NotNull ModuleDescription getDescription() {
        return description;
    }

    /**
     * Get a stream of a resource
     *
     * @param resource the resource name
     * @return the resource
     */
    @Override
    public @NotNull Optional<ResourceHandle> getResource(String resource) {
        return Optional.empty();
    }

    /**
     * Get the plugin network
     *
     * @return the network
     */
    @Override
    public @NotNull PluginNetwork getNetwork() {
        return network;
    }

    /**
     * Get the module loader
     *
     * @return the loader
     */
    @Override
    public @NotNull ModuleManager getManager() {
        return loader;
    }

    /**
     * Returns whether this module is
     * enabled
     *
     * @return the module status
     */
    @Override
    public boolean isEnabled() {
        return maker.isImplemented(plugin);
    }

    /**
     * The actions to perform when the module
     * gets loaded
     */
    @Override
    public void onLoad() {

    }

    /**
     * The actions to perform when the module
     * gets enabled
     */
    @Override
    public void onEnable() {
        if (onModEnable != null) {
            try {
                onModEnable.invoke(this);
            } catch (Throwable ex) {
                throw new RuntimeException("An exception occurred while execute plugin module enable handler");
            }
        }
    }

    /**
     * The actions to perform when the module
     * gets disabled
     */
    @Override
    public void onDisable() {
        if (onModDisable != null) {
            try {
                onModDisable.invoke(this);
            } catch (Throwable ex) {
                throw new RuntimeException("An exception occurred while execute plugin module disable handler");
            }
        }
    }

    /**
     * Get the plugin that instantiates the
     * module
     *
     * @return the plugin instantiating the
     * module
     */
    @Override
    public @NotNull JavaPlugin getPlugin() {
        return plugin;
    }
}
