package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.Cancellable;
import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.NetworkEntity;

/**
 * When an entity pre connects the server
 */
public class EntityPreConnectEvent extends EntityEvent implements Cancellable {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    private boolean cancelled = false;
    private String reason = "";

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent()}
     */
    public EntityPreConnectEvent(NetworkEntity entity) throws SecurityException {
        super(entity);
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntityPreConnectEvent(Module caller, NetworkEntity entity) throws SecurityException {
        super(caller, entity);
    }

    /**
     * Cancel the event
     *
     * @param cancel if the event is cancelled
     * @param reason the cancel reason
     */
    @Override
    public void setCancelled(final boolean cancel, final String reason) {
        this.cancelled = cancel;
        this.reason = reason;
    }

    /**
     * Get if the event is cancelled
     *
     * @return if the event is cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Get the event cancel reason
     *
     * @return the cancel reason
     */
    @Override
    public String cancelReason() {
        return reason;
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
