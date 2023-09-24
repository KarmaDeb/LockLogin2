package es.karmadev.locklogin.api.extension.module.exception;

/**
 * This exception is thrown when a module
 * has not a valid description
 */
public class InvalidDescriptionException extends Exception {

    /**
     * Initialize the exception
     *
     * @param ex the parent exception
     */
    public InvalidDescriptionException(final Throwable ex) {

    }

    /**
     * Initialize the exception
     *
     * @param message the exception message
     */
    public InvalidDescriptionException(final String message) {
        super(message);
    }
}
