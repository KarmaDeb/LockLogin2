package es.karmadev.locklogin.api.network.client.event;

import es.karmadev.locklogin.api.event.Cancellable;
import es.karmadev.locklogin.api.network.NetworkEntity;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

/**
 * When a client destroy his account
 */
public class ClientDestroyEvent implements ClientEvent, Cancellable {

    private boolean cancelled = false;
    private String reason = null;

    private final LocalNetworkClient client;
    private final NetworkEntity issuer;

    /**
     * Initialize the event
     *
     * @param client the client
     */
    public ClientDestroyEvent(final LocalNetworkClient client, final NetworkEntity issuer) {
        this.client = client;
        this.issuer = issuer;
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
     * Get the issuer of the account
     * removal
     *
     * @return the removal issuer
     */
    public NetworkEntity issuer() {
        return issuer;
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
