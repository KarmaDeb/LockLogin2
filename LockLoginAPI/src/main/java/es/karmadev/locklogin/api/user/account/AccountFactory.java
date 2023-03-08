package es.karmadev.locklogin.api.user.account;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

import java.util.UUID;

/**
 * LockLogin account factory
 */
public interface AccountFactory<T extends UserAccount> {

    /**
     * Create an account for the specified client
     *
     * @param client the client to generate
     *               the account for
     * @return the client account
     */
    T create(final LocalNetworkClient client);
}
