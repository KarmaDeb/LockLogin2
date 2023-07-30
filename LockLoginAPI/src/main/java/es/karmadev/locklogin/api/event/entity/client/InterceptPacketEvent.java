package es.karmadev.locklogin.api.event.entity.client;

import es.karmadev.locklogin.api.event.Cancellable;
import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.event.handler.EventHandlerList;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This event is fired when a packet
 * gets intercepted by the plugin
 */
@RequiredArgsConstructor
public class InterceptPacketEvent extends LockLoginEvent implements Cancellable {

    private final static EventHandlerList HANDLER_LIST = new EventHandlerList();

    private boolean cancelled = false;
    private String reason;

    @Getter
    private final NetworkClient client;
    @Getter
    private final String packetName;
    @Getter
    private final Object packet;

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
