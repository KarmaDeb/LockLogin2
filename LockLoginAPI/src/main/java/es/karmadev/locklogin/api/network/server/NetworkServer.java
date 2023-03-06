package es.karmadev.locklogin.api.network.server;

import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannel;

/**
 * Network server
 */
public interface NetworkServer extends NetworkEntity {

    /**
     * Get the server id
     *
     * @return the server id
     */
    int id();

    /**
     * Get the server packet queue
     *
     * @return the server packet queue
     */
    NetworkChannel channel();
}
