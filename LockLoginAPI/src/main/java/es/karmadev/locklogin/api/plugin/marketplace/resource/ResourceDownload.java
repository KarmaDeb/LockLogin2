package es.karmadev.locklogin.api.plugin.marketplace.resource;

import es.karmadev.api.schedule.task.completable.late.LateTask;

/**
 * Represents a {@link MarketResource resource} download
 * information
 */
public interface ResourceDownload {

    /**
     * Get the file
     *
     * @return the file name
     */
    String getFile();

    /**
     * Get the file size
     *
     * @return the file size
     */
    long getSize();

    /**
     * Download the file
     *
     * @return if the file was able to be downloaded
     */
    LateTask<Boolean> download();
}
