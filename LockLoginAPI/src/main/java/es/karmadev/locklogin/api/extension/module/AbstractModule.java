package es.karmadev.locklogin.api.extension.module;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.PluginNetwork;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * The abstract module. This is the class modules must
 * extend in order to be detected by the plugin as modules
 */
public abstract class AbstractModule implements Module {

    private boolean enabled = false;
    private ModuleLoader loader = null;
    private ModuleDescription description = null;
    private Path file = null;
    private Path dataFolder = null;
    private ClassLoader classLoader;
    private final PluginNetwork network = CurrentPlugin.getPlugin().network();

    /**
     * Initializes the module
     */
    public AbstractModule() {
        final ClassLoader loader = this.getClass().getClassLoader();
        if (!(loader instanceof ModuleClassLoader)) {
            throw new IllegalStateException("AbstractModule requires ModuleClassLoader");
        }

        ((ModuleClassLoader) loader).initialize();
    }

    /**
     * Initialize the module
     *
     * @param loader the module loader
     * @param description the module description
     * @param file the module file
     * @param dataFolder the module data folder
     */
    protected AbstractModule(final @NotNull ModuleLoader loader, final @NotNull ModuleDescription description, final @NotNull Path file, final @NotNull Path dataFolder) {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        init(loader, description, file, dataFolder, classLoader);
    }

    final void init(final @NotNull ModuleLoader loader, final @NotNull ModuleDescription description, final @NotNull Path file, final @NotNull Path dataFolder, final @NotNull ClassLoader classLoader) {
        this.loader = loader;
        this.description = description;
        this.file = file;
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
    }

    /**
     * Get the module name
     *
     * @return the module name
     */
    @Override
    public final String getName() {
        return Module.super.getName();
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
        return dataFolder;
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
    public @NotNull ModuleLoader getLoader() {
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
        return enabled;
    }

    /**
     * Set the enable status of the module
     *
     * @param status the status
     */
    protected final void setEnabled(final boolean status) {
        if (enabled != status) {
            enabled = status;

            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    /**
     * Get the module class loader
     *
     * @return the class loader
     */
    protected final ClassLoader getClassLoader() {
        return classLoader;
    }
}
