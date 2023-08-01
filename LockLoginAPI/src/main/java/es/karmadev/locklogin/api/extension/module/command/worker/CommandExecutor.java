package es.karmadev.locklogin.api.extension.module.command.worker;

import es.karmadev.locklogin.api.network.NetworkEntity;

/**
 * LockLogin module command execturo
 */
public interface CommandExecutor {

    /**
     * Execute the command
     *
     * @param entity the command executor
     * @param command the command name
     * @param arguments the command arguments
     */
    void execute(final NetworkEntity entity, final String command, final String[] arguments);
}
