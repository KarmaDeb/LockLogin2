package es.karmadev.locklogin.api.extension.module.command.error;

/**
 * This exception is thrown when a command fails to execute
 * for any reason
 */
public class CommandRuntimeException extends RuntimeException {

    /**
     * Initialize the exception
     *
     * @param sup the error tha caused the exception
     * @param message the error message
     */
    public CommandRuntimeException(final Throwable sup, final String message) {
        super(message);
        if (sup != null) addSuppressed(sup);
    }
}
