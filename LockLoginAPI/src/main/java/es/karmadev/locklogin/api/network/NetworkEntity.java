package es.karmadev.locklogin.api.network;

import es.karmadev.locklogin.api.network.client.data.PermissionObject;

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

    /**
     * Get if this entity has the specified permission
     *
     * @param permission the permission
     * @return if the entity has the perimssion
     */
    boolean hasPermission(final PermissionObject permission);
}
