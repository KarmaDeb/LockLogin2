package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin message configuration
 */
public interface MessageIntervalSection extends Serializable {

    /**
     * Get the registration interval
     *
     * @return the registration interval
     */
    int registration();

    /**
     * Get the logging interval
     *
     * @return the logging interval
     */
    int logging();
}
