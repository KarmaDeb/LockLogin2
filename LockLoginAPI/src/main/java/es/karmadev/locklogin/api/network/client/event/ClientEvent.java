package es.karmadev.locklogin.api.network.client.event;

import es.karmadev.locklogin.api.event.LockLoginEvent;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

public interface ClientEvent extends LockLoginEvent {

    /**
     * Get the event client
     *
     * @return the event client
     */
    LocalNetworkClient getClient();
}
