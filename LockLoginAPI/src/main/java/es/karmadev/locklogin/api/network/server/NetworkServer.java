package es.karmadev.locklogin.api.network.server;

import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.TextContainer;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannel;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * Network server
 */
@SuppressWarnings("unused")
public interface NetworkServer extends NetworkEntity, TextContainer {

    /**
     * Get the server id
     *
     * @return the server id
     */
    int id();

    /**
     * Update the server name
     *
     * @param name the server name
     */
    void setName(final String name);

    /**
     * Update the server address
     *
     * @param address the server new address
     */
    void setAddress(final InetSocketAddress address);

    /**
     * Get all the clients that are connected
     * in this server
     *
     * @return all the connected clients
     */
    Collection<NetworkClient> connected();

    /**
     * Get all the offline clients that
     * are connected in this server
     *
     * @return all the offline clients
     */
    Collection<LocalNetworkClient> offlineClients();

    Collection<NetworkClient> onlineClients();

    /**
     * Get the server packet queue
     *
     * @return the server packet queue
     */
    NetworkChannel channel();
}
