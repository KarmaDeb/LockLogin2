package es.karmadev.locklogin.api.network;

import java.net.InetSocketAddress;
import java.time.Instant;

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

    /**
     * Get when the entity was created
     *
     * @return the entity creation date
     */
    Instant creation();
}
