package es.karmadev.locklogin.api.plugin.runtime.dependency;

import es.karmadev.locklogin.api.plugin.runtime.dependency.shade.RelocationSet;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * LockLogin dependency
 */
public interface LockLoginDependency {

    /**
     * Get the dependency type
     *
     * @return the type
     */
    DependencyType type();

    /**
     * Get the dependency id
     *
     * @return the dependency id
     */
    String id();

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
     * Get the dependency relocations
     *
     * @return the dependency relocations
     */
    @NotNull
    RelocationSet getRelocations();

    /**
     * Get the dependency dependencies
     *
     * @return the dependencies
     */
    @NotNull
    List<String> getDependencies();

    /**
     * Get the dependency download URL
     *
     * @return the dependency download URL
     */
    URL downloadURL();

    /**
     * Get if the dependency is a plugin
     *
     * @return if the dependency is a plugin
     * @deprecated See {@link LockLoginDependency#type()}
     */
    @Deprecated
    boolean isPlugin();
}
