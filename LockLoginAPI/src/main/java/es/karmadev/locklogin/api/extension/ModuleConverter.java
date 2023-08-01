package es.karmadev.locklogin.api.extension;

import es.karmadev.locklogin.api.extension.plugin.PluginModule;

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
    PluginModule<T> extend(final T plugin);

    /**
     * Undo the plugin module extension
     *
     * @param module the extension
     */
    void retract(final PluginModule<T> module);

    /**
     * Undo the plugin module extension
     *
     * @param plugin the plugin owning the
     *               extension
     */
    void retract(final T plugin);

    /**
     * Get if the plugin is extended
     *
     * @param plugin the plugin
     * @return if the plugin is extended
     */
    boolean isExtended(final T plugin);
}
