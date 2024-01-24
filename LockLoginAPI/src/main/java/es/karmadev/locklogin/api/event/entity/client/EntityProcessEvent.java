package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.entity.EntityEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;
import lombok.Getter;

/**
 * Represents when an entity is authenticated, only by the completion of
 * a {@link es.karmadev.locklogin.api.user.auth.process.UserAuthProcess}. Unlike
 * {@link EntityAuthenticateEvent}, this event is fired RIGHT AFTER the user
 * goes to the next authentication process, so it cannot be cancelled. The
 * main difference is from where those events gets cancelled. Meanwhile, this event is
 * called after {@link UserAuthProcess#process(AuthProcess) process future} gets completed,
 * {@link EntityAuthenticateEvent} gets called as soon as the plugin detects the user has
 * completed all the auth process or has failed in any of them (which usually triggers a
 * client kick).
 */
@Getter
public class EntityProcessEvent extends EntityEvent {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    private final boolean success;
    private final UserAuthProcess process;

    /**
     * Initialize the entity event
     *
     * @param entity the entity
     * @param success if the authentication success
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent()}
     */
    public EntityProcessEvent(final NetworkEntity entity, final boolean success, final UserAuthProcess process) throws SecurityException {
        super(entity);
        this.success = success;
        this.process = process;
    }

    /**
     * Initialize the entity event
     *
     * @param caller the event caller
     * @param entity the entity
     * @param success if the authentication success
     * @throws SecurityException as part of {@link LockLoginEvent#LockLoginEvent(Module)}
     */
    public EntityProcessEvent(final Module caller, final NetworkEntity entity, final boolean success, final UserAuthProcess process) throws SecurityException {
        super(caller, entity);
        this.success = success;
        this.process = process;
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
