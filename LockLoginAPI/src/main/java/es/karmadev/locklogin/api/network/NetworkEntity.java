package es.karmadev.locklogin.api.network;

import es.karmadev.locklogin.api.network.client.data.PermissionObject;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.UUID;

/**
 * Network entity
 */
public interface NetworkEntity extends NetComponent {

    /**
     * Get the entity id
     *
     * @return the entity id
     */
    int id();

    /**
     * Get the entity unique id
     *
     * @return the entity unique id
     */
    default UUID uniqueId() {
        return UUID.randomUUID();
    }

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
     * @return if the entity has the permission
     */
    boolean hasPermission(final PermissionObject permission);
}
