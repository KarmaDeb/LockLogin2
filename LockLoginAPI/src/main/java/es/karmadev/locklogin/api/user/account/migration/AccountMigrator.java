package es.karmadev.locklogin.api.user.account.migration;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.AccountField;
import es.karmadev.locklogin.api.user.account.UserAccount;

/**
 * Plugin account migrator
 */
public interface AccountMigrator<T extends UserAccount> {

    /**
     * Migrate an account
     *
     * @param owner the account owner
     * @param transictionable the transictionable account
     * @param ignore the fields to ignore
     * @return the migrated account
     */
    T migrate(final LocalNetworkClient owner, final Transictionable transictionable, final AccountField... ignore);

    /**
     * Export an account into a transictionable
     * account format
     *
     * @param account the account to export
     * @return the transictionable account
     */
    Transictionable export(final T account);
}
