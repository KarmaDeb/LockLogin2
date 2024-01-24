package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin encryption configuration
 */
public interface EncryptionConfiguration extends Serializable {

    /**
     * Get the algorithm used for hashing
     *
     * @return the hashing algorithm
     */
    String algorithm();

    /**
     * Get if the results should have base64
     *
     * @return if the results are base64 strings
     */
    boolean applyBase64();

    /**
     * Get if the plugin applies virtual ID
     * to passwords
     *
     * @return if the plugin protects passwords with
     * a virtual id
     */
    boolean virtualID();

    /**
     * Get the minimum memory to use
     *
     * @return the minimum memory
     */
    int memory();

    /**
     * Get the parallelism level
     *
     * @return the parallelism level
     */
    int parallelism();


    /**
     * Get the minimum amount of iterations
     *
     * @return the minimum amount of iterations
     */
    int iterations();
}
