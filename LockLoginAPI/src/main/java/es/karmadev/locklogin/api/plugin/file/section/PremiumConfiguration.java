package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * Premium configuration section
 */
public interface PremiumConfiguration extends Serializable {

    /**
     * Get if the plugin will use its integrated
     * premium system
     *
     * @return if the plugin toggles premium
     */
    boolean enable();

    /**
     * Get if the plugin keeps the clients offline
     * UUIDs
     *
     * @return if the clients should still have their
     * offline UUIDs
     */
    boolean forceOfflineId();
}
