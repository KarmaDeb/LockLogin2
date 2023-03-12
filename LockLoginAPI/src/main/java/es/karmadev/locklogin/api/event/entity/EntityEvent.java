package es.karmadev.locklogin.api.event.entity;

import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.network.NetworkEntity;

/**
 * An event that involves an entity
 */
public abstract class EntityEvent extends LockLoginEvent {

    protected final NetworkEntity entity;

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent()}
     */
    public EntityEvent(final NetworkEntity entity) throws SecurityException {
        this(null, entity);
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntityEvent(final Module caller, final NetworkEntity entity) throws SecurityException {
        super(caller);
        this.entity = entity;
    }

    /**
     * Get the entity involved in this event
     *
     * @return the entity event
     */
    public NetworkEntity getEntity() {
        return entity;
    }
}
