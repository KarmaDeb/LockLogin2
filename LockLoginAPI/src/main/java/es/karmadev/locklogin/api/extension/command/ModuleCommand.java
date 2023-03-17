package es.karmadev.locklogin.api.extension.command;

import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.extension.command.worker.CommandCompletor;
import es.karmadev.locklogin.api.extension.command.worker.CommandExecutor;
import lombok.Getter;

/**
 * Module command
 */
@SuppressWarnings("unused")
public abstract class ModuleCommand {

    @Getter
    private final Module module;
    @Getter
    private final String name;
    @Getter
    private final String description;
    @Getter
    private final String[] aliases;

    /**
     * Initialize the command
     *
     * @param module the command module
     * @param cmd the command name
     * @param description the command description
     * @param aliases the command aliases
     */
    public ModuleCommand(final Module module, final String cmd, final String description, final String... aliases) {
        this.module = module;
        name = cmd;
        this.description = description;
        this.aliases = aliases;
    }

    /**
     * Set the command executor
     *
     * @param executor the command executor
     * @return this command
     */
    public abstract ModuleCommand setExecutor(final CommandExecutor executor);

    /**
     * Set the command tab completor
     *
     * @param completor the command completor
     * @return this command
     */
    public abstract ModuleCommand setTabCompletor(final CommandCompletor completor);

    /**
     * Get the command executor
     *
     * @return the command executor
     */
    public abstract CommandExecutor getExecutor();

    /**
     * Get the command tab completor
     *
     * @return the command tab completor
     */
    public abstract CommandCompletor getTabCompletor();
}
