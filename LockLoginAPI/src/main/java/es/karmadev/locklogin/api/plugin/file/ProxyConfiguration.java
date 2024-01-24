package es.karmadev.locklogin.api.plugin.file;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;

/**
 * LockLogin proxy servers configuration
 */
public interface ProxyConfiguration {

    /**
     * Reload the proxy configuration
     *
     * @return if the configuration could
     * be reloaded
     */
    boolean reload();

    /**
     * Get if the proxy performs server
     * verification
     *
     * @return if the proxy performs server
     * validation
     */
    boolean checkServers();

    /**
     * Get all the lobby servers
     *
     * @return the lobby servers
     */
    NetworkServer[] lobbyServers();

    /**
     * Get all the auth servers
     *
     * @return the auth servers
     */
    NetworkServer[] authServers();

    /**
     * Get all the premium servers
     *
     * @return the premium servers
     */
    NetworkServer[] premiumServers();

    /**
     * Check if the client is in a server
     *
     * @param client if the client is in a server
     * @return if in server
     */
    default boolean isInLobby(final LocalNetworkClient client) {
        NetworkServer server = client.server();
        for (NetworkServer target : lobbyServers()) {
            if (target.id() == server.id()) return true;
        }

        return false;
    }

    /**
     * Check if the client is in a server
     *
     * @param client if the client is in a server
     * @return if in server
     */
    default boolean isInAuth(final LocalNetworkClient client) {
        NetworkServer server = client.server();
        for (NetworkServer target : authServers()) {
            if (target.id() == server.id()) return true;
        }

        return false;
    }

    /**
     * Check if the client is in a server
     *
     * @param client if the client is in a server
     * @return if in server
     */
    default boolean isInPremium(final LocalNetworkClient client) {
        NetworkServer server = client.server();
        for (NetworkServer target : premiumServers()) {
            if (target.id() == server.id()) return true;
        }

        return false;
    }
}
