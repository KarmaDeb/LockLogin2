package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

/**
 * This event is fired when an entity is created
 */
@SuppressWarnings("unused")
public class EntityCreatedEvent extends EntityEvent {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent()}
     */
    public EntityCreatedEvent(final LocalNetworkClient entity) throws SecurityException {
        this(null, entity);
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntityCreatedEvent(final Module caller, final LocalNetworkClient entity) throws SecurityException {
        super(caller, entity);
    }

    /**
     * Get the entity involved in this event
     *
     * @return the entity event
     */
    @Override
    public LocalNetworkClient getEntity() {
        return (entity != null ? (LocalNetworkClient) entity : null);
    }

    /**
     * Get all the handlers for this
     * event
     *
     * @return the event handlers
     */
    public static EventHandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
