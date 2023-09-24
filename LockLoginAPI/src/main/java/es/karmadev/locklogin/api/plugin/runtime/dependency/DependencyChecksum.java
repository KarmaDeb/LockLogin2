package es.karmadev.locklogin.api.plugin.runtime.dependency;

/**
 * Dependency checksum data
 */
public interface DependencyChecksum {

    /**
     * Get the checksum value
     *
     * @param name the check name
     * @return the checksum value
     */
    long value(final String name);

    /**
     * Get the checksum hash
     *
     * @return the check hash
     */
    String hash();

    /**
     * Verifies if the provided checksum matches the
     * current one
     *
     * @param other the other checksum
     * @return if the checksums are the same
     */
    boolean matches(final DependencyChecksum other);
}
