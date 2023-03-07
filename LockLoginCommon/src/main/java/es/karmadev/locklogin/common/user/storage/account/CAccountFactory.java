package es.karmadev.locklogin.common.user.storage.account;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.AccountFactory;

public class CAccountFactory implements AccountFactory<CAccount> {

    /**
     * Create an account for the specified client
     *
     * @param client the client
     * @return the client account
     */
    @Override
    public CAccount create(final LocalNetworkClient client) {
        return null;
    }

    /**
     * Create an account for the specified player name
     * <p>
     * PLEASE NOTE: This method must handle the following:
     * - Verify if a player with that name exists, if so, assing the new
     * account to its id
     * - If a player with name does not exists, insert it into the database
     * and assign the account to its id
     *
     * @param name the player name
     * @return the player account
     */
    @Override
    public CAccount create(final String name) {
        return null;
    }
}
