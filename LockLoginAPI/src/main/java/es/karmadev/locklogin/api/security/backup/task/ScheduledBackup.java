package es.karmadev.locklogin.api.security.backup.task;

import es.karmadev.locklogin.api.security.backup.store.BackupStorage;

import java.util.function.Consumer;

/**
 * Scheduled backup task
 */
public interface ScheduledBackup {

    /**
     * Get the backup id
     *
     * @return the backup id
     */
    String id();

    /**
     * When the task starts
     *
     * @param action the action to perform
     */
    void onStart(final Runnable action);

    /**
     * When an exception raises during backup
     *
     * @param error the exception
     */
    void onException(final Consumer<Throwable> error);

    /**
     * When the backup task as finished
     *
     * @param storage the backup storage
     */
    void onFinish(final Consumer<BackupStorage> storage);
}
