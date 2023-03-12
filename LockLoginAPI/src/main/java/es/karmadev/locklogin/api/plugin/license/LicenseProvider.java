package es.karmadev.locklogin.api.plugin.license;

import java.nio.file.Path;
import java.util.UUID;

/**
 * LockLogin license provider
 */
public interface LicenseProvider {

    /**
     * Update the license
     *
     * @param license the license to update
     * @return the updated license
     */
    License update(final Path license);

    /**
     * Update the license
     *
     * @param license the license to update
     * @return the update fields
     */
    String[] update(final License license);

    /**
     * Fetch a license from a file
     *
     * @param file the license file
     * @return the license
     *
     * @throws SecurityException if the license load source is not the plugin
     */
    License load(final Path file) throws SecurityException;

    /**
     * Synchronize with a license
     *
     * @param key the license synchronization key
     * @return the synchronized license
     */
    License synchronize(final String key);

    /**
     * Synchronize with a license
     *
     * @param key the license key
     * @param username the license owner
     * @param password the license password
     * @return the synchronized license
     *
     * @throws SecurityException if the license request source is not the plugin
     */
    License synchronize(final String key, final String username, final String password);

    /**
     * Request a new license
     *
     * @return a new free license
     */
    License request() throws SecurityException;

    /**
     * Request a new license
     *
     * @param id the license id
     * @param username the license owner
     * @param password the license password
     * @return the license
     *
     * @throws SecurityException if the license request source is not the plugin
     */
    License request(final UUID id, final String username, final String password) throws SecurityException;
}
