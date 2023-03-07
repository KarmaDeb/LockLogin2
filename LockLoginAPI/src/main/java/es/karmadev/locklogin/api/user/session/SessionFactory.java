package es.karmadev.locklogin.api.user.session;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

/**
 * LockLogin session factory
 */
public interface SessionFactory<T extends UserSession> {

    /**
     * Create a session for the specified client
     *
     * @param client the client
     * @return the client session
     */
    T create(final LocalNetworkClient client);

    /**
     * Create a session for the specified player name
     *
     * PLEASE NOTE: This method must handle the following:
     * - Verify if a player with that name exists, if so, assing the new
     * session to its id
     * - If a player with name does not exists, insert it into the database
     * and assign the session to its id
     *
     * @param name the player name
     * @return the player session
     */
    T create(final String name);
}
