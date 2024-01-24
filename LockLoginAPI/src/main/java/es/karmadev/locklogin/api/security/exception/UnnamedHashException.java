package es.karmadev.locklogin.api.security.exception;

import es.karmadev.locklogin.api.security.hash.PluginHash;

/**
 * This exception is thrown when a plugin hash is tried
 * to be registered with an invalid name
 */
public class UnnamedHashException extends Exception {

    /**
     * Initialize the exception
     *
     * @param hash the hash
     */
    public UnnamedHashException(final PluginHash hash) {
        super("Cannot register hash " + hash.getClass().getName() + " because it has an invalid name");
    }

    /**
     * Initialize the exception
     */
    public UnnamedHashException() {
        super("Tried to register a null hash method");
    }
}
