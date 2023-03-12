package es.karmadev.locklogin.api.plugin.license.data;

/**
 * License owner information
 */
public interface LicenseOwner {

    /**
     * Get the license owner
     *
     * @return the license owner
     */
    String name();

    /**
     * Get the license contact information
     *
     * @return the license contact
     */
    String contact();
}
