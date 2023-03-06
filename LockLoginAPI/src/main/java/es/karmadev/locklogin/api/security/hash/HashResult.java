package es.karmadev.locklogin.api.security.hash;

import es.karmadev.locklogin.api.security.virtual.VirtualizedInput;

import java.io.Serializable;

/**
 * Hash result
 */
public interface HashResult extends Serializable {

    /**
     * Get the hasher
     *
     * @return the hasher
     */
    PluginHash hasher();

    /**
     * Get the final hash
     *
     * @return the hash
     */
    VirtualizedInput product();
}
