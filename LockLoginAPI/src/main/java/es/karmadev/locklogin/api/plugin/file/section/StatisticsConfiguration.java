package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * Statistics configuration section
 */
public interface StatisticsConfiguration extends Serializable {

    /**
     * Get if the plugin shares metrics with
     * bstats
     *
     * @return if the plugin shares data
     * with bstats
     */
    boolean shareBStats();

    /**
     * Get if the plugin shared data should
     * be public
     *
     * @return if the shared data is public
     */
    boolean publicLockLogin();
}
