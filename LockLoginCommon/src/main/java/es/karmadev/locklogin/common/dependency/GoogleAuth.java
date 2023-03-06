package es.karmadev.locklogin.common.dependency;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.plugin.runtime.dependency.DependencyChecksum;
import es.karmadev.locklogin.api.plugin.runtime.dependency.LockLoginDependency;

import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GoogleAuth implements LockLoginDependency {

    private final static Checksum checksum = new Checksum();
    private final static Checksum current_checksum = new Checksum();

    /**
     * Get the dependency name
     *
     * @return the dependency name
     */
    @Override
    public String name() {
        return "2fa authentication";
    }

    /**
     * Get the dependency file
     *
     * @return the dependency file
     */
    @Override
    public Path file() {
        return CurrentPlugin.getPlugin().workingDirectory().resolve("dependencies").resolve("GoogleAuthenticator.jar");
    }

    /**
     * Get the dependency checksum
     *
     * @return the dependency checksum
     */
    @Override
    public Checksum checksum() {
        return checksum;
    }

    /**
     * Generate a checksum for the current dependency file
     *
     * @return the dependency checksum
     */
    @Override
    public Checksum generateChecksum() {
        return current_checksum;
    }

    /**
     * Get the dependency download URL
     *
     * @return the dependency download URL
     */
    @Override
    public URL downloadURL() {
        return null;
    }
}