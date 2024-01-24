package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.plugin.service.mail.MailMessage;

/**
 * This event is exclusively fired when
 * a {@link MailMessage message} is received to
 * a client. Implementations must fire this event
 */
public class EntityMailReceiveEvent extends EntityEvent {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    private final MailMessage message;

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @param message the mail message
     */
    public EntityMailReceiveEvent(final NetworkEntity entity, final MailMessage message) {
        this(null, entity, message);
    }

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @param message the mail message
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntityMailReceiveEvent(final Module module, final NetworkEntity entity, final MailMessage message) throws SecurityException {
        super(module, entity);
        this.message = message;
    }

    /**
     * Get the mail message
     *
     * @return the mail message
     */
    public MailMessage getMessage() {
        return message;
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
