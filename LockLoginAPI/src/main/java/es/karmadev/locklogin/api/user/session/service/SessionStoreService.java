package es.karmadev.locklogin.api.user.session.service;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.service.PluginService;

import java.net.InetSocketAddress;

/**
 * The session store service is
 * the service which manages a persistent
 * session. This service should be able
 * to determine if a client who left the server
 * is suitable to restore his last session
 * status
 */
public interface SessionStoreService extends PluginService {

    /**
     * Get the session of an address
     *
     * @param address the address
     * @return the session
     */
    SessionCache getSession(final InetSocketAddress address);

    /**
     * Save the session of a client
     *
     * @param client the client to save
     *               session for
     */
    void saveSession(final LocalNetworkClient client);
}
