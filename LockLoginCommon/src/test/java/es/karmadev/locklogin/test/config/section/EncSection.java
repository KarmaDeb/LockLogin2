package es.karmadev.locklogin.test.config.section;

import es.karmadev.locklogin.api.plugin.file.section.EncryptionConfiguration;

public class EncSection implements EncryptionConfiguration {

    /**
     * Get the algorithm used for passwords
     *
     * @return the password algorithm
     */
    @Override
    public String algorithm() {
        return "argon2d";
    }

    /**
     * Get if the results should have base64
     *
     * @return if the results are base64 strings
     */
    @Override
    public boolean applyBase64() {
        return true;
    }

    /**
     * Get if the plugin applies virtual ID
     * to passwords
     *
     * @return if the plugin protects passwords with
     * a virtual id
     */
    @Override
    public boolean virtualID() {
        return true;
    }

    /**
     * Get the minimum memory to use
     *
     * @return the minimum memory
     */
    @Override
    public int memory() {
        return 1024;
    }

    /**
     * Get the parallelism level
     *
     * @return the parallelism level
     */
    @Override
    public int parallelism() {
        return 22;
    }

    /**
     * Get the minimum amount of iterations
     *
     * @return the minimum amount of iterations
     */
    @Override
    public int iterations() {
        return 2;
    }
}
