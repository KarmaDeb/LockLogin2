package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin sessions configuration
 */
public interface SessionConfiguration extends Serializable {

    /**
     * Get if the plugin should allow persistent
     * sessions based on IP
     *
     * @return if the plugin uses sessions
     */
    boolean enable();

    /**
     * Get the session max time
     *
     * @return the session max time
     */
    int timeout();
}
