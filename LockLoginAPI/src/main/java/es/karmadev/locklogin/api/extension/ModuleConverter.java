package es.karmadev.locklogin.api.extension;

import es.karmadev.locklogin.api.extension.module.PluginModule;

/**
 * LockLogin module converter
 */
public interface ModuleConverter<T> {

    /**
     * Extend the plugin into a plugin module
     *
     * @param plugin the plugin to extend
     * @return the extended module
     */
    PluginModule<T> implement(final T plugin);

    /**
     * Undo the plugin module extension
     *
     * @param module the extension
     */
    void dispose(final PluginModule<T> module);

    /**
     * Undo the plugin module extension
     *
     * @param plugin the plugin owning the
     *               extension
     */
    void dispose(final T plugin);

    /**
     * Get if the plugin is extended
     *
     * @param plugin the plugin
     * @return if the plugin is extended
     */
    boolean isImplemented(final T plugin);
}
