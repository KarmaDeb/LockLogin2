package es.karmadev.locklogin.api.security.backup.store;

import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;

import java.time.Instant;

/**
 * Backup storage
 */
public interface BackupStorage {

    /**
     * Get the backup id
     *
     * @return the backup id
     */
    String id();

    /**
     * Get all the backups
     *
     * @return the backups
     */
    UserBackup[] accounts();

    /**
     * Find a backup for the client
     *
     * @param client the client
     * @return the client backup if any
     */
    UserBackup find(final LocalNetworkClient client);

    /**
     * Get when the backup was created
     *
     * @return the backup creation date
     */
    Instant creation();

    /**
     * Destroy the backup
     *
     * @return if the backup could be removed
     */
    boolean destroy();
}
