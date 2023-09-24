package es.karmadev.locklogin.api.plugin.runtime.dependency;

/**
 * LockLogin dependency types
 */
public enum DependencyType {
    /**
     * Pack of dependencies (.zip file)
     */
    PACKAGE,
    /**
     * Single dependency (.jar file)
     */
    SINGLE,
    /**
     * Plugin dependency (platform-dependent)
     */
    PLUGIN
}
