package es.karmadev.locklogin.api.network.client.event;

import es.karmadev.locklogin.api.event.Cancellable;
import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

/**
 * When a client joins the server
 */
public class ClientPostJoinEvent implements ClientEvent, Cancellable {

    private boolean cancelled = false;
    private String reason = null;

    private NetworkClient client;

    /**
     * Initialize the event
     *
     * @param client the client
     */
    public ClientPostJoinEvent(final NetworkClient client) {
        this.client = client;
    }

    /**
     * Get the event client
     *
     * @return the event client
     */
    @Override
    public LocalNetworkClient getClient() {
        return client;
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
}
