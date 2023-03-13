package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.session.UserSession;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * This event is fired when an entity session is created.
 * This doesn't mean the client has been logged in any method,
 * but his session is ready to handle it.
 */
@SuppressWarnings("unused")
public class EntitySessionCreatedEvent extends EntityEvent {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    @Nullable
    @Getter
    private final UserSession session;

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @param session the entity session
     * @throws SecurityException as part of {@link  LockLoginEvent#LockLoginEvent()}
     */
    public EntitySessionCreatedEvent(final LocalNetworkClient entity, final UserSession session) throws SecurityException {
        this(null, entity, session);
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @param session the entity session
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntitySessionCreatedEvent(final Module caller, final LocalNetworkClient entity, UserSession session) throws SecurityException {
        super(caller, entity);
        this.session = session;
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
