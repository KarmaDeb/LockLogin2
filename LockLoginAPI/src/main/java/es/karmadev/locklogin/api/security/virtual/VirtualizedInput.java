package es.karmadev.locklogin.api.security.virtual;

import java.io.Serializable;

/**
 * Virtualized input
 */
public interface VirtualizedInput extends Serializable {

    /**
     * Get the virtualization references
     *
     * @return the references
     */
    int[] references();

    /**
     * Get if the virtualization input is
     * valid. If false, then it means no virtualization
     * has been applied
     *
     * @return if virtualization was applied
     */
    boolean valid();

    /**
     * Get the virtualization product
     *
     * @return the virtualization product
     */
    byte[] product();
}
