package es.karmadev.locklogin.common.api.plugin.service;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.service.PluginService;

/**
 * LockLogin spartan anti cheat compatibility service
 */
public class SpartanService implements PluginService {

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "Spartan AntiCheat service";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return false;
    }

    /**
     * Verify a client
     *
     * @param client the client to verify
     * @return if the client is valid
     */
    public boolean verify(final LocalNetworkClient client) {
        return true;
    }
}
