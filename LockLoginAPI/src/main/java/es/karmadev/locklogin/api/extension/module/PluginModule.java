package es.karmadev.locklogin.api.extension.module;

import org.jetbrains.annotations.NotNull;

/**
 * A plugin that extends a module
 */
public interface PluginModule<T> extends Module {

    /**
     * Get the plugin that instantiates the
     * module
     *
     * @return the plugin instantiating the
     * module
     */
    @NotNull
    T getPlugin();
}
