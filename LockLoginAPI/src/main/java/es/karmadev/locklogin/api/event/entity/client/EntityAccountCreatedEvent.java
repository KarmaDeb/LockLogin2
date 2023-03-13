package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.UserAccount;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * This event is fired when an entity account is created.
 * This doesn't mean the client is registered, but his account
 * is ready to handle it
 */
@SuppressWarnings("unused")
public class EntityAccountCreatedEvent extends EntityEvent {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    @Nullable
    @Getter
    private final UserAccount account;

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @param account the entity account
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent()}
     */
    public EntityAccountCreatedEvent(final LocalNetworkClient entity, final UserAccount account) throws SecurityException {
        this(null, entity, account);
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @param account the entity account
     * @throws SecurityException as part of {@link es.karmadev.locklogin.api.event.LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntityAccountCreatedEvent(final Module caller, final LocalNetworkClient entity, final UserAccount account) throws SecurityException {
        super(caller, entity);
        this.account = account;
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
