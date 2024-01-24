package es.karmadev.locklogin.api.extension.module.command;

/**
 * Command registrar
 */
public interface CommandRegistrar {

    /**
     * Find a command
     *
     * @param name the command name
     * @return the command
     */
    ModuleCommand getCommand(final String name);

    /**
     * Get all the commands
     *
     * @return the commands
     */
    ModuleCommand[] getCommands();
}
