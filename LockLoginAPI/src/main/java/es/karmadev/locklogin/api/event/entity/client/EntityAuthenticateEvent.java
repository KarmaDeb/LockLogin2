package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.locklogin.api.event.Cancellable;
import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.NetworkEntity;
import lombok.Getter;

/**
 * Represents when an entity is authenticated, only after all the
 * auth process handlers are completed. This event gets fired after
 * the client session values gets updated. Unlike {@link EntityProcessEvent},
 * this event is not attached to any {@link es.karmadev.locklogin.api.user.auth.process.UserAuthProcess}
 */
public class EntityAuthenticateEvent extends EntityEvent implements Cancellable {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    private boolean cancelled = false;
    @Getter
    private final boolean success;
    private String reason = null;

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @param success if the authentication success
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent()}
     */
    public EntityAuthenticateEvent(final NetworkEntity entity, final boolean success) throws SecurityException {
        super(entity);
        this.success = success;
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @param success if the authentication success
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntityAuthenticateEvent(final Module caller, final NetworkEntity entity, final boolean success) throws SecurityException {
        super(caller, entity);
        this.success = success;
    }

    /**
     * Cancel the event
     *
     * @param cancel if the event is cancelled
     * @param reason the cancel reason
     */
    @Override
    public void setCancelled(final boolean cancel, final String reason) {
        if (cancel) {
            if (ObjectUtils.isNullOrEmpty(reason)) return;

            this.cancelled = true;
            this.reason = reason;
            return;
        }

        this.cancelled = false;
        this.reason = null;
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
