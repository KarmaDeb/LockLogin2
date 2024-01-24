package es.karmadev.locklogin.api.plugin;

import java.io.Serializable;
import java.time.Instant;

/**
 * LockLogin server hash
 */
public interface ServerHash extends Serializable {

    /**
     * Hash value
     *
     * @return the hash value
     */
    String value();

    /**
     * Get the hash creation time
     *
     * @return the hash time
     */
    Instant creation();
}
