package es.karmadev.locklogin.common.api.extension.command;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.extension.module.command.CommandRegistrar;
import es.karmadev.locklogin.api.extension.module.command.ModuleCommand;
import es.karmadev.locklogin.common.api.extension.CModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CCommandMap implements CommandRegistrar {

    public final Map<Module, Map<ModuleCommand, Set<String>>> commands = new ConcurrentHashMap<>();
    private final CModuleManager manager;

    public CCommandMap(final CModuleManager manager) {
        this.manager = manager;
    }

    /**
     * Register a command
     *
     * @param module the module owning the command
     * @param command the command
     */
    public void register(final Module module, final ModuleCommand command) {
        if (manager.onCommandRegistered != null && manager.onCommandRegistered.apply(command)) {
            String name = command.getName();
            Map<ModuleCommand, Set<String>> commandMap = commands.computeIfAbsent(module, (m) -> new ConcurrentHashMap<>());
            Set<String> knownCommands = commandMap.computeIfAbsent(command, (m) -> ConcurrentHashMap.newKeySet());

            knownCommands.add(name);
            knownCommands.add(module.getName() + ":" + name);
            for (String alias : command.getAliases()) {
                knownCommands.add(module.getName() + ":" + alias);
            }
            commandMap.put(command, knownCommands);
            commands.put(module, commandMap);
        }
    }

    /**
     * Unregister all the commands of the module
     *
     * @param module the module commands
     */
    public void unregisterAll(final Module module) {
        Map<ModuleCommand, Set<String>> commandMap = commands.get(module);
        if (commandMap == null) return;

        commandMap.keySet().forEach((command) -> {
            if (manager.onCommandUnregistered != null) manager.onCommandUnregistered.accept(command);
        });
        commands.remove(module);
    }

    /**
     * Find a command
     *
     * @param name the command name
     * @return the command
     */
    @Override
    public ModuleCommand getCommand(final String name) {
        for (Module module : commands.keySet()) {
            Map<ModuleCommand, Set<String>> commandMap = commands.get(module);
            if (commandMap == null || commandMap.isEmpty()) continue;

            for (ModuleCommand command : commandMap.keySet()) {
                Set<String> knownCommands = commandMap.get(command);
                if (knownCommands.stream().anyMatch(name::equalsIgnoreCase)) return command;
            }
        }

        return null;
    }

    /**
     * Get all the commands
     *
     * @return the commands
     */
    @Override
    public ModuleCommand[] getCommands() {
        List<ModuleCommand> list = new ArrayList<>();
        for (Module module : commands.keySet()) {
            Map<ModuleCommand, Set<String>> commandMap = commands.get(module);
            if (commandMap == null || commandMap.isEmpty()) continue;

            list.addAll(commandMap.keySet());
        }

        return list.toArray(new ModuleCommand[0]);
    }
}

