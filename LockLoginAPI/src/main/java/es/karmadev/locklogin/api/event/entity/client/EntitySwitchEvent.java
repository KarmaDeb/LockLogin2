package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.Cancellable;
import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.network.server.NetworkServer;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * This event is fired when an entity is connected
 */
@SuppressWarnings("unused")
public class EntitySwitchEvent extends EntityEvent implements Cancellable {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    @Getter
    @Nullable
    private final NetworkServer from;
    @Getter
    private final NetworkServer to;

    private boolean cancelled = false;
    private String reason = null;

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @param from the server the client is comming from
     * @param to the server the client has been connected to
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent()}
     * @throws IllegalArgumentException as part of {@link EntitySwitchEvent(LocalNetworkClient, NetworkServer, NetworkServer)}
     */
    public EntitySwitchEvent(final LocalNetworkClient entity, final NetworkServer from, final NetworkServer to) throws SecurityException, IllegalArgumentException {
        this(null, entity, from, to);
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @param from the server the client is comming from
     * @param to the server the client has been connected to
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent(Module)}
     * @throws IllegalArgumentException if the target server is not valid
     */
    public EntitySwitchEvent(final Module caller, final LocalNetworkClient entity, final @Nullable NetworkServer from, final NetworkServer to) throws SecurityException, IllegalArgumentException {
        super(caller, entity);
        this.from = from;
        this.to = to;
        if (to == null) throw new IllegalArgumentException("Cannot create EntitySwitchEvent without a valid target server");
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
     * Cancel the event
     *
     * @param cancel if the event is cancelled
     * @param reason the cancel reason
     */
    @Override
    public void setCancelled(final boolean cancel, final String reason) {
        cancelled = cancel;
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
