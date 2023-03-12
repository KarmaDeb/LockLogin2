package es.karmadev.locklogin.api.security.backup.task;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Backup restore task
 */
public interface BackupRestoreTask {

    /**
     * After the backup has been restored
     *
     * @param success the success consumer
     */
    void onSuccess(final Consumer<Integer> success);

    /**
     * After any error has been raised
     *
     * @param fail the error consumer
     */
    void onFail(final BiConsumer<Throwable, Integer> fail);
}
