package es.karmadev.locklogin.common.plugin.service.backup;

import es.karmadev.locklogin.api.security.backup.store.BackupStorage;
import es.karmadev.locklogin.api.security.backup.task.ScheduledBackup;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

public class CBackupTask implements ScheduledBackup {

    private final String id;

    protected Runnable start;
    protected Consumer<Throwable> error;
    protected Consumer<BackupStorage> success;

    /**
     * Create the backup task
     *
     * @param id the task id
     */
    public CBackupTask(final String id) {
        this.id = id;
    }

    /**
     * Get the backup id
     *
     * @return the backup id
     */
    @Override
    public String id() {
        return id;
    }

    /**
     * When the task starts
     *
     * @param action the action to perform
     */
    @Override
    public void onStart(final Runnable action) {
        start = action;
    }

    /**
     * When an exception raises during backup
     *
     * @param error the exception
     */
    @Override
    public void onException(final Consumer<Throwable> error) {
        this.error = error;
    }

    /**
     * When the backup task as finished
     *
     * @param storage the backup storage
     */
    @Override
    public void onFinish(final Consumer<BackupStorage> storage) {
        success = storage;
    }
}
