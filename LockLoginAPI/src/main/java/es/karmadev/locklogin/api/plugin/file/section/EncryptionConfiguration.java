package es.karmadev.locklogin.api.plugin.file.section;

import java.io.Serializable;

/**
 * LockLogin encryption configuration
 */
public interface EncryptionConfiguration extends Serializable {

    /**
     * Get the algorithm used for passwords
     *
     * @return the password algorithm
     */
    String passwordAlgorithm();

    /**
     * Get the algorithm used for
     * pins
     *
     * @return the pin algorithm
     */
    String pinAlgorithm();

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
}
