package es.karmadev.locklogin.api.plugin.license;

import es.karmadev.locklogin.api.plugin.license.data.LicenseExpiration;
import es.karmadev.locklogin.api.plugin.license.data.LicenseOwner;

import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

/**
 * LockLogin license information
 */
public interface License {

    /**
     * Get the license version
     *
     * @return the license version
     */
    String version();

    /**
     * Get the license file location
     *
     * @return the license file location
     */
    Path location();

    /**
     * Set the license location
     *
     * @param location the license location
     * @throws NotDirectoryException if the location is not a directory
     */
    void setLocation(final Path location) throws NotDirectoryException;

    /**
     * Get the license base64 value
     *
     * @return the license base64 value
     */
    String base64();

    /**
     * Get the license synchronization key
     *
     * @return the license synchronization key
     */
    String synchronizationKey();

    /**
     * Get the license communication key
     *
     * @return the license communication key
     */
    String communicationKey();

    /**
     * Get the license owner
     *
     * @return the license owner
     */
    LicenseOwner owner();

    /**
     * Get when this license expires
     *
     * @return the license expiration time
     */
    LicenseExpiration expiration();

    /**
     * Get the maximum amount of proxies
     * allowed in this license
     *
     * @return the proxies amount
     */
    int maxProxies();

    /**
     * Get the license backup storage
     * size
     *
     * @return the license backup storage
     */
    long backupStorage();

    /**
     * Get if the license is free
     *
     * @return if the license is free
     */
    boolean free();

    /**
     * Get if the license is installed
     *
     * @return the license install status
     */
    boolean installed();

    /**
     * Install the license
     *
     * @return if the license was able to be installed
     */
    boolean install();

    /**
     * Force the installation of this license, this won't
     * make checks for if the license file exists. Use with
     * caution
     */
    void forceInstall();

    /**
     * Merge the other license with this one
     *
     * @param other the other license
     * @return the updated fields
     */
    String[] merge(final License other);
}
