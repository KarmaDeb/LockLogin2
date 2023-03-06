package es.karmadev.locklogin.api.plugin.runtime.dependency;

import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

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
}
