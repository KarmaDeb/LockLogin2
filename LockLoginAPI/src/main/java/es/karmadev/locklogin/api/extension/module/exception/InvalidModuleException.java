package es.karmadev.locklogin.api.extension.module.exception;

/**
 * This exception is thrown when a module
 * is not valid
 */
public class InvalidModuleException extends Exception {

    /**
     * Initialize the exception
     *
     * @param ex the parent exception
     */
    public InvalidModuleException(final Throwable ex) {
        super(ex);
    }

    /**
     * Initialize the exception
     *
     * @param message the message
     */
    public InvalidModuleException(final String message) {
        super(message);
    }

    /**
     * Initializes the exception
     *
     * @param message the message
     * @param ex the parent exception
     */
    public InvalidModuleException(final String message, final Throwable ex) {
        super(message, ex);
    }
}
