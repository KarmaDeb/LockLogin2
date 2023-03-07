package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin backup configuration
 */
public interface BackupConfiguration extends Serializable {

    /**
     * Get if the backups are enabled
     *
     * @return if backups are enabled
     */
    boolean enabled();

    /**
     * Get the maximum amount of backups
     *
     * @return the maximum amount of backups
     */
    int max();

    /**
     * Get the backup creation period
     *
     * @return the backup creation period
     */
    int period();

    /**
     * Get the backup auto purge interval
     *
     * @return the backup auto purge interval
     */
    int purge();
}
