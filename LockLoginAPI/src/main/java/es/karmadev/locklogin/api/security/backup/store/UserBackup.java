package es.karmadev.locklogin.api.security.backup.store;

import es.karmadev.locklogin.api.user.account.migration.Transitional;

import java.time.Instant;

/**
 * User backup data
 */
public interface UserBackup extends Transitional {

    /**
     * Get the transictionable account id
     *
     * @return the account id
     */
    int account();

    /**
     * Get when the account was created
     *
     * @return the account creation date
     */
    Instant creation();
}
