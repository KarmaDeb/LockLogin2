package es.karmadev.locklogin.api.network;

import java.net.InetSocketAddress;

/**
 * Network entity
 */
public interface NetworkEntity {

    /**
     * Get the entity name
     *
     * @return the entity name
     */
    String name();

    /**
     * Get the entity address
     *
     * @return the entity address
     */
    InetSocketAddress address();


}
