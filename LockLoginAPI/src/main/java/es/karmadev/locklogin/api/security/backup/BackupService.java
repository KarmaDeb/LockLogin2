package es.karmadev.locklogin.api.security.backup;

import es.karmadev.locklogin.api.security.backup.store.BackupStorage;
import es.karmadev.locklogin.api.security.backup.task.BackupRestoreTask;
import es.karmadev.locklogin.api.security.backup.task.ScheduledBackup;

import java.time.Instant;

/**
 * LockLogin backup service
 */
public interface BackupService {

    /**
     * Perform a backup
     *
     * @return the backup task
     */
    ScheduledBackup performBackup();

    /**
     * Perform a backup
     *
     * @param id the backup id
     * @return the backup task
     */
    ScheduledBackup performBackup(final String id);

    /**
     * Get all the backups
     *
     * @return all the backups
     */
    BackupStorage[] fetchAll();

    /**
     * Get a backup
     *
     * @param id the backup id
     * @return the backup
     */
    BackupStorage fetch(final String id);

    /**
     * Purge all the backups
     *
     * @return the amount of purged
     * backups
     */
    int purgeAll();

    /**
     * Purge all the backups between the provided
     * dates
     *
     * @param start the start date
     * @param end the end date
     * @return the amount of purged
     * backups
     */
    int purgeBetween(final Instant start, final Instant end);

    /**
     * Purge all the backups made after
     * the specified date
     *
     * @param start the start date
     * @return the purged backups
     */
    int purgeSince(final Instant start);

    /**
     * Purge all the backups made before
     * the specified date
     *
     * @param end the end date
     * @return the purged backups
     */
    int purgeUntil(final Instant end);

    /**
     * Restore all the backups
     *
     * @param method the restore method
     * @return the restore task
     */
    BackupRestoreTask restoreAll(final RestoreMethod method);

    /**
     * Restore a backup
     *
     * @param backup the backup to restore
     * @param method the restore method
     * @return the restore task
     */
    BackupRestoreTask restore(final BackupStorage backup, final RestoreMethod method);

    /**
     * Restore all the backups between the provided ones
     *
     * @param from the backup to start from
     * @param to the backup to end at
     * @param method the restore method
     * @return the restore task
     */
    BackupRestoreTask restore(final BackupStorage from, final BackupStorage to, final RestoreMethod method);

    /**
     * Restore all the backups made since the
     * specified backup
     *
     * @param start the start backup
     * @param method the restore method
     * @return the restore task
     */
    BackupRestoreTask restoreSince(final BackupStorage start, final RestoreMethod method);

    /**
     * Restore all the backups made until the
     * specified backup
     *
     * @param end the end backup
     * @param method the restore method
     * @return the restore task
     */
    BackupRestoreTask restoreUntil(final BackupStorage end, final RestoreMethod method);
}
