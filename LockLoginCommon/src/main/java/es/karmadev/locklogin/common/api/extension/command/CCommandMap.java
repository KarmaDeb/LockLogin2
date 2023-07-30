package es.karmadev.locklogin.common.api.extension.command;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.extension.command.CommandRegistrar;
import es.karmadev.locklogin.api.extension.command.ModuleCommand;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;
import es.karmadev.locklogin.common.api.extension.CModuleManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CCommandMap implements CommandRegistrar {

    public final Map<String, ModuleCommand> commands = new ConcurrentHashMap<>();
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
            commands.put(module.sourceName() + ":" + name, command);
        }
    }

    /**
     * Unregister all the commands of the module
     *
     * @param module the module commands
     */
    public void unregisterAll(final Module module) {
        List<String> remove = new ArrayList<>();

        String name = module.sourceName();
        for (String key : commands.keySet()) {
            if (key.startsWith(name + ":")) {
                remove.add(key);
            }
        }

        for (String key : remove) {
            ModuleCommand command = commands.remove(key);
            if (manager.onCommandUnregistered != null) manager.onCommandUnregistered.accept(command);
        }
    }

    /**
     * Find a command
     *
     * @param name the command name
     * @return the command
     */
    @Override
    public ModuleCommand getCommand(final String name) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        LockLoginRuntime runtime = plugin.getRuntime();
        String tmpName = name;

        if (!name.contains(":")) {
            Path caller = runtime.caller();
            Module module = manager.loader().findByFile(caller);
            if (module == null) throw new RuntimeException("Cannot get command from invalid module");

            tmpName = module.sourceName() + ":" + name;
        } else {
            String[] data = name.split(":");
            String modName = data[0];
            String rawName = name.replaceFirst(modName + ":", "");

            Path caller = runtime.caller();
            Module module = manager.loader().findByFile(caller);
            Module cmdMod = manager.loader().findByName(modName);

            if (module == null) throw new RuntimeException("Cannot get command from invalid module");
            if (cmdMod == null) throw new IllegalArgumentException("Unknown command: " + rawName);
            if (!module.equals(cmdMod)) throw new SecurityException("Illegal command access");
        }

        return commands.getOrDefault(tmpName, null);
    }

    /**
     * Get all the commands
     *
     * @return the commands
     */
    @Override
    public ModuleCommand[] getCommands() {
        return commands.values().toArray(new ModuleCommand[0]);
    }
}

