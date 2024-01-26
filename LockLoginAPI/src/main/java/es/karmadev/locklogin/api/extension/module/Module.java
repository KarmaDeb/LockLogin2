package es.karmadev.locklogin.api.extension.module;

import es.karmadev.locklogin.api.extension.module.resource.ResourceHandle;
import es.karmadev.locklogin.api.network.PluginNetwork;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents a LockLogin module
 */
@SuppressWarnings("unused")
public interface Module {

    /**
     * Get the module name
     *
     * @return the module name
     */
    default String getName() {
        return getDescription().getName();
    }

    /**
     * Get the module version
     *
     * @return the module version
     */
    default String getVersion() {
        return getDescription().getVersion();
    }

    /**
     * Get if the module is from the
     * marketplace
     *
     * @return if the module is from the marketplace
     */
    boolean isFromMarketplace();

    /**
     * Get the module file
     *
     * @return the module file
     */
    @NotNull
    Path getFile();

    /**
     * Get the module data folder
     *
     * @return the module data folder
     */
    @NotNull
    Path getDataFolder();

    /**
     * Get the module logger
     *
     * @return the module logger
     */
    ModuleLogger getLogger();

    /**
     * Get the module description
     *
     * @return the module description
     */
    @NotNull
    ModuleDescription getDescription();

    /**
     * Get a stream of a resource
     *
     * @param resource the resource name
     * @return the resource
     */
    @NotNull
    Optional<ResourceHandle> getResource(final String resource);

    /**
     * Tries to close a resource
     *
     * @param handle the handle to close
     * @return if the handle was successfully closed, implementations
     * might also implement some kind of Map containing a resource handle,
     * and it's keyed resource string and remove the resource from it
     * when this method gets called
     */
    default boolean closeResource(final ResourceHandle handle) {
        if (!handle.getModule().equals(this) || handle.isOpen()) return false; //Already closed or we don't own it

        handle.close();
        return !handle.isOpen(); //If it's open, we failed, otherwise, we succeed
    }

    /**
     * Get the plugin network
     *
     * @return the network
     */
    @NotNull
    PluginNetwork getNetwork();

    /**
     * Get the module loader
     *
     * @return the loader
     */
    @NotNull
    ModuleManager getManager();

    /**
     * Returns whether this module is
     * enabled
     *
     * @return the module status
     */
    boolean isEnabled();

    /**
     * The actions to perform when the module
     * gets loaded
     */
    void onLoad();

    /**
     * The actions to perform when the module
     * gets enabled
     */
    void onEnable();

    /**
     * The actions to perform when the module
     * gets disabled
     */
    void onDisable();
}
