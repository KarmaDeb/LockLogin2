package es.karmadev.locklogin.api.plugin.runtime.dependency;

/**
 * Dependency checksum data
 */
public interface DependencyVersion {

    /**
     * Get the dependency project version
     *
     * @return the dependency version
     */
    String project();

    /**
     * Get the dependency plugin instance version.
     * By default the dependency uses the same proejct version
     * if the dependency is not a plugin
     *
     * @return the dependency plugin version
     */
    String plugin();
}
