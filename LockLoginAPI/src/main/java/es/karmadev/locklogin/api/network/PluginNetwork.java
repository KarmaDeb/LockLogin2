package es.karmadev.locklogin.api.network;


import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Plugin network
 */
@SuppressWarnings("unused")
public interface PluginNetwork {

    /**
     * Get a client
     *
     * @param id the client id
     * @return the client
     */
    NetworkClient getPlayer(final int id);

    /**
     * Get a client
     *
     * @param name the client name
     * @return the client
     */
    NetworkClient getPlayer(final String name);

    /**
     * Get a client
     *
     * @param uniqueId the client unique id
     * @return the client
     */
    NetworkClient getPlayer(final UUID uniqueId);

    /**
     * Get a client
     *
     * @param id the client id
     * @return the client
     */
    LocalNetworkClient getEntity(final int id);

    /**
     * Get an offline client
     *
     * @param uniqueId the client unique id
     * @return the client
     */
    LocalNetworkClient getOfflinePlayer(final UUID uniqueId);

    /**
     * Get an offline client
     *
     * @param id the client id
     * @return the client
     */
    LocalNetworkClient getOfflinePlayer(final int id);

    /**
     * Get a server
     *
     * @param id the server id
     * @return the server
     */
    NetworkServer getServer(final int id);

    /**
     * Get a server
     *
     * @param name the server name
     * @return the server
     */
    NetworkServer getServer(final String name);

    /**
     * Get all the online players
     *
     * @return the online players
     */
    Collection<NetworkClient> getOnlinePlayers();

    /**
     * Get all the players
     *
     * @return all the players
     */
    Collection<LocalNetworkClient> getPlayers();

    /**
     * Get all the servers
     *
     * @return the servers
     */
    Collection<NetworkServer> getServers();
}
