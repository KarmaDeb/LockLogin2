package es.karmadev.locklogin.api.user.session;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

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
}
