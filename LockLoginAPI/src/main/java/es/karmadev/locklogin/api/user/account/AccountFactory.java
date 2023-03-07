package es.karmadev.locklogin.api.user.account;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

/**
 * LockLogin account factory
 */
public interface AccountFactory<T extends UserAccount> {

    /**
     * Create an account for the specified client
     *
     * @param client the client
     * @return the client account
     */
    T create(final LocalNetworkClient client);

    /**
     * Create an account for the specified player name
     *
     * PLEASE NOTE: This method must handle the following:
     * - Verify if a player with that name exists, if so, assing the new
     * account to its id
     * - If a player with name does not exists, insert it into the database
     * and assign the account to its id
     *
     * @param name the player name
     * @return the player account
     */
    T create(final String name);
}
