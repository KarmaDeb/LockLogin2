package es.karmadev.locklogin.api.security;

import es.karmadev.locklogin.api.security.exception.UnnamedHashException;
import es.karmadev.locklogin.api.security.hash.PluginHash;
import es.karmadev.locklogin.api.security.virtual.VirtualID;

/**
 * LockLogin hasher
 */
@SuppressWarnings("unused")
public interface LockLoginHasher {

    /**
     * Register a new hash method
     *
     * @param hash the hash method
     * @throws UnnamedHashException if the hash method is unnamed
     */
    void registerMethod(final PluginHash hash) throws UnnamedHashException;

    /**
     * Tries to unregister the hash method
     *
     * @param name the hash method name to unregister
     * @return if the method could be removed
     */
    boolean unregisterMethod(final String name);

    /**
     * Get the hashing method
     *
     * @param name the hash method
     * @return the hashing method
     */
    PluginHash getMethod(final String name);

    /**
     * Get all the compatible hashing method
     *
     * @return the hashing methods
     */
    String[] getMethods();

    /**
     * Get the plugin virtual ID
     *
     * @return the plugin virtual ID
     * @throws SecurityException if tried to access from a non
     * module or jar file
     */
    VirtualID virtualID() throws SecurityException;
}
