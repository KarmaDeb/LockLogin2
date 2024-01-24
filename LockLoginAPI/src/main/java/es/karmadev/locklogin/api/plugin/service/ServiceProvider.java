package es.karmadev.locklogin.api.plugin.service;

/**
 * LockLogin service provider
 */
public interface ServiceProvider<T extends PluginService> extends PluginService {

    /**
     * Serve a plugin service
     *
     * @param arguments the service arguments
     * @return the plugin service
     */
    T serve(final Object... arguments);

    /**
     * Get the service class
     *
     * @return the service class
     */
    Class<T> getService();
}
