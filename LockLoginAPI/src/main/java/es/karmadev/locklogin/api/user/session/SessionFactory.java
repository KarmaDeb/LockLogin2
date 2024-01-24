package es.karmadev.locklogin.api.user.session;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

import java.util.Collection;

/**
 * LockLogin session factory
 */
public interface SessionFactory<T extends UserSession> {

    /**
     * Create a session for the specified client
     *
     * @param client the client to generate
     *               the session for
     * @return the client session
     */
    T create(final LocalNetworkClient client);

    /**
     * Get all the sessions on the server
     *
     * @return all the server sessions
     */
    Collection<T> getSessions();
}
