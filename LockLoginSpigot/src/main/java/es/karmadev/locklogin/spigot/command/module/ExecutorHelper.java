package es.karmadev.locklogin.spigot.command.module;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Command executor helper
 */
public class ExecutorHelper implements CommandExecutor {

    private final Consumer<PluginCommand> issuer;

    private ExecutorHelper(final Consumer<PluginCommand> issuer) {
        this.issuer = issuer;
    }

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String[] args) {
        PluginCommand pc = PluginCommand.of(sender, label, args);
        if (issuer != null) issuer.accept(pc);

        return false;
    }

    public static ExecutorHelper createExecutor(final Consumer<PluginCommand> consumer) {
        return new ExecutorHelper(consumer);
    }
}
