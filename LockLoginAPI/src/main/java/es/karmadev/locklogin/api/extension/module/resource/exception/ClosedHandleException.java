package es.karmadev.locklogin.api.extension.module.resource.exception;

import es.karmadev.locklogin.api.extension.module.resource.ResourceHandle;

import java.io.IOException;

/**
 * This exception is thrown when a call to
 * {@link ResourceHandle#createStream()} is made but the
 * original stream has been closed
 */
public class ClosedHandleException extends IOException {

    /**
     * Initializes the exception
     *
     * @param handle the handle
     */
    public ClosedHandleException(final ResourceHandle handle) {
        super("Cannot create an input stream of " + handle.getName() + " because it's closed");
    }
}
