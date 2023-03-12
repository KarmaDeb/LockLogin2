package es.karmadev.locklogin.common.plugin.service.backup;

import es.karmadev.locklogin.api.security.backup.task.BackupRestoreTask;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CRestoreTask implements BackupRestoreTask {

    protected Consumer<Integer> success;
    protected BiConsumer<Throwable, Integer> error;

    /**
     * After the backup has been restored
     *
     * @param success the success consumer
     */
    @Override
    public void onSuccess(final Consumer<Integer> success) {
        this.success = success;
    }

    /**
     * After any error has been raised
     *
     * @param fail the error consumer
     */
    @Override
    public void onFail(final BiConsumer<Throwable, Integer> fail) {
        this.error = fail;
    }
}
