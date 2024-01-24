package es.karmadev.locklogin.api.user.account;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.migration.AccountMigrator;

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

    /**
     * Get all the accounts
     *
     * @return all the plugin accounts
     */
    T[] getAllAccounts();

    /**
     * Get the account migrator of this factory
     *
     * @return the factory account migrator
     */
    AccountMigrator<T> migrator();
}
