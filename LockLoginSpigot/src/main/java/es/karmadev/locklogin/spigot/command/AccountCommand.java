package es.karmadev.locklogin.spigot.command;

import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@PluginCommand(command = "account") @SuppressWarnings("unused")
public class AccountCommand extends Command {

    protected AccountCommand(final @NotNull String name) {
        super(name);
    }

    /**
     * Executes the command, returning its success
     *
     * @param sender       Source object which is executing this command
     * @param label        The alias of the command used
     * @param args         All arguments passed to the command, split via ' '
     * @return true if the command was successful, otherwise false
     */
    @Override
    public boolean execute(final @NotNull CommandSender sender, final @NotNull String label, final @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

        }

        return false;
    }
}
