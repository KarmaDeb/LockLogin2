package es.karmadev.locklogin.api.plugin.runtime.dependency;

import java.net.URL;
import java.nio.file.Path;

/**
 * LockLogin dependency
 */
public interface LockLoginDependency {

    /**
     * Get the dependency name
     *
     * @return the dependency name
     */
    String name();

    /**
     * Get the class to test with if
     * the dependency exists
     *
     * @return the dependency test class
     */
    String testClass();

    /**
     * Get the dependency version
     *
     * @return the dependency version
     */
    DependencyVersion version();

    /**
     * Get the dependency file
     *
     * @return the dependency file
     */
    Path file();

    /**
     * Get the dependency checksum
     *
     * @return the dependency checksum
     */
    DependencyChecksum checksum();

    /**
     * Generate a checksum for the current dependency file
     *
     * @return the dependency checksum
     */
    DependencyChecksum generateChecksum();

    /**
     * Get the dependency download URL
     *
     * @return the dependency download URL
     */
    URL downloadURL();

    /**
     * Get if the dependency is a plugin
     *
     * @return if the dependenc is a plugin
     */
    boolean isPlugin();

    /**
     * Get if the dependency is installed
     *
     * @return if the dependency is installed
     */
    boolean needsInstallation();

    /**
     * Mark the dependency as installed
     */
    void assertInstalled();
}
