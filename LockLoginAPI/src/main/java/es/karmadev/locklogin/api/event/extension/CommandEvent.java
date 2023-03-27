package es.karmadev.locklogin.api.event.extension;

import es.karmadev.locklogin.api.event.Cancellable;

/**
 * LockLogin command event
 */
public interface CommandEvent extends Cancellable {

    /**
     * Get the command message
     *
     * @return the command message
     */
    String getMessage();

    /**
     * Get the command
     *
     * @return the command
     */
    String getCommand();

    /**
     * Get the command arguments
     *
     * @return the arguments
     */
    String[] getArguments();

    /**
     * Set the command
     *
     * @param command the new command
     */
    void setCommand(final String command);
}
