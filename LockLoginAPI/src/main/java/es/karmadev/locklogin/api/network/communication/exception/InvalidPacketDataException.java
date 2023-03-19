package es.karmadev.locklogin.api.network.communication.exception;

import com.google.gson.JsonSyntaxException;

/**
 * This exception is thrown when a packet data
 * is not in a valid json data. LockLogin uses
 * json packets to handle data send/receive from
 * different server instances.
 */
public class InvalidPacketDataException extends Exception {

    /**
     * Initialize the exception
     *
     * @param message the exception message
     */
    public InvalidPacketDataException(final String message) {
        super(message);
    }

    /**
     * Initialize the exception
     *
     * @param raw the raw json
     */
    public InvalidPacketDataException(final JsonSyntaxException raw) {
        super("Cannot parse packet data");
        addSuppressed(new JsonSyntaxException(raw));
    }
}
