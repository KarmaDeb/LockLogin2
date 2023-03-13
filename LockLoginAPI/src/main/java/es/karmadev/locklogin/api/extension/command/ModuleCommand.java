package es.karmadev.locklogin.api.extension.command;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.extension.command.worker.CommandCompletor;
import es.karmadev.locklogin.api.extension.command.worker.CommandExecutor;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import lombok.Getter;

import java.nio.file.Path;

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

    @Getter
    private CommandExecutor executor;

    @Getter
    private CommandCompletor tabCompletor;

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
     * @throws SecurityException if the module trying to define the
     * executor is not the command owner
     */
    public void setExecutor(final CommandExecutor executor) throws SecurityException {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.runtime();

        Path caller = runtime.caller();
        Module callerModule = plugin.moduleManager().loader().find(caller);

        if (callerModule == null || !callerModule.equals(module)) {
            throw new SecurityException("Cannot set module executor from an unverified source");
        }

        this.executor = executor;
    }

    /**
     * Set the command tab completor
     *
     * @param completor the command executor
     * @throws SecurityException if the module trying to define the
     * executor is not the command owner
     */
    public void setTabCompletor(final CommandCompletor completor) throws SecurityException {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.runtime();

        Path caller = runtime.caller();
        Module callerModule = plugin.moduleManager().loader().find(caller);

        if (callerModule == null || !callerModule.equals(module)) {
            throw new SecurityException("Cannot set module tab completor from an unverified source");
        }

        this.tabCompletor = completor;
    }
}
