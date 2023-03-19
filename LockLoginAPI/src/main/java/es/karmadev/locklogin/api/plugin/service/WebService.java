package es.karmadev.locklogin.api.plugin.service;

/**
 * LockLogin web service
 */
public interface WebService extends PluginService {

    /**
     * Install this service
     */
    void install();

    /**
     * Enable this service
     *
     * @return if the service was able to be
     * enabled
     */
    boolean enable();

    /**
     * Disable this service
     *
     * @return if the service was able to
     * be disabled
     */
    boolean disable();
}
