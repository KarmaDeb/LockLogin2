package es.karmadev.locklogin.common.api.extension.command;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.command.ModuleCommand;
import es.karmadev.locklogin.api.extension.module.command.worker.CommandCompletor;
import es.karmadev.locklogin.api.extension.module.command.worker.CommandExecutor;

public class CModCommand extends ModuleCommand {

    private final Module module;

    private CommandExecutor executor;
    private CommandCompletor tabCompletor;

    /**
     * Initialize the command
     *
     * @param module      the command module
     * @param cmd         the command name
     * @param description the command description
     * @param aliases     the command aliases
     */
    public CModCommand(final Module module, final String cmd, final String description, final String... aliases) {
        super(module, cmd, description, aliases);
        this.module = module;
    }

    /**
     * Set the command executor
     *
     * @param executor the command executor
     * @throws SecurityException if the module trying to define the
     * executor is not the command owner
     */
    public ModuleCommand setExecutor(final CommandExecutor executor) throws SecurityException {
        this.executor = executor;
        return this;
    }

    /**
     * Set the command tab completor
     *
     * @param completor the command executor
     * @throws SecurityException if the module trying to define the
     * executor is not the command owner
     */
    public ModuleCommand setTabCompletor(final CommandCompletor completor) throws SecurityException {
        this.tabCompletor = completor;
        return this;
    }

    /**
     * Get the command executor
     *
     * @return the command executor
     */
    @Override
    public CommandExecutor getExecutor() {
        return executor;
    }

    /**
     * Get the command tab completor
     *
     * @return the command tab completor
     */
    @Override
    public CommandCompletor getTabCompletor() {
        return tabCompletor;
    }
}
