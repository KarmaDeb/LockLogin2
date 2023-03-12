package es.karmadev.locklogin.common.user.storage.account.transiction;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.AccountField;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.account.migration.AccountMigrator;
import es.karmadev.locklogin.api.user.account.migration.Transictionable;
import es.karmadev.locklogin.common.SQLiteDriver;
import es.karmadev.locklogin.common.user.storage.account.CAccount;

public class CMigrator implements AccountMigrator<CAccount> {

    private final SQLiteDriver driver;

    public CMigrator(final SQLiteDriver driver) {
         this.driver = driver;
    }

    /**
     * Migrate an account
     *
     * @param owner           the account owner
     * @param transictionable the transictionable account
     * @param ignore          the fields to ignore
     * @return the migrated account
     */
    @Override
    public CAccount migrate(final LocalNetworkClient owner, final Transictionable transictionable, final AccountField... ignore) {

    }

    /**
     * Export an account into a transictionable
     * account format
     *
     * @param account the account to export
     * @return the transictionable account
     */
    @Override
    public Transictionable export(final CAccount account) {
        return CTransictionable.from(account);
    }
}
