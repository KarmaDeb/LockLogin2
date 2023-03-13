package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.Cancellable;
import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;

/**
 * This event is fired when an entity recives a validation
 * of any type
 */
public class EntityValidationEvent extends EntityEvent implements Cancellable {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    private boolean cancelled = false;
    private String reason;

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent()}
     */
    public EntityValidationEvent(final LocalNetworkClient entity) throws SecurityException, IllegalArgumentException {
        this(null, entity);
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntityValidationEvent(final Module caller, final LocalNetworkClient entity) throws SecurityException {
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
        cancelled = cancel;
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
