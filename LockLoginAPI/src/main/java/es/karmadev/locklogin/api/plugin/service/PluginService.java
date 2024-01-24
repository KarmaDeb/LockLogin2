package es.karmadev.locklogin.api.plugin.service;

/**
 * LockLogin service
 */
public interface PluginService {

    /**
     * Get the service name
     *
     * @return the service name
     */
    String name();

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    boolean useProvider();
}
