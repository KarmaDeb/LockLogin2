package es.karmadev.locklogin.common.api.plugin.file;

import es.karmadev.locklogin.api.network.server.NetworkServer;
import es.karmadev.locklogin.api.plugin.file.ProxyConfiguration;

class CProxyConfiguration implements ProxyConfiguration {


    /**
     * Reload the proxy configuration
     *
     * @return if the configuration could
     * be reloaded
     */
    @Override
    public boolean reload() {
        return false;
    }

    /**
     * Get if the proxy performs server
     * verification
     *
     * @return if the proxy performs server
     * validation
     */
    @Override
    public boolean checkServers() {
        return false;
    }

    /**
     * Get all the lobby servers
     *
     * @return the lobby servers
     */
    @Override
    public NetworkServer[] lobbyServers() {
        return new NetworkServer[0];
    }

    /**
     * Get all the auth servers
     *
     * @return the auth servers
     */
    @Override
    public NetworkServer[] authServers() {
        return new NetworkServer[0];
    }

    /**
     * Get all the premium servers
     *
     * @return the premium servers
     */
    @Override
    public NetworkServer[] premiumServers() {
        return new NetworkServer[0];
    }
}
