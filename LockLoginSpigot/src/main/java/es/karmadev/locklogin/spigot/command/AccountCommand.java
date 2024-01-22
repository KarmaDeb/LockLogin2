package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.api.spigot.inventory.helper.func.Action;
import es.karmadev.api.spigot.inventory.helper.option.OptionsInventory;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.window.settings.SettingsButton;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@PluginCommand(command = "account") @SuppressWarnings("unused")
public class AccountCommand extends Command {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();

    public AccountCommand(final @NotNull String name) {
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
            Messages messages = plugin.messages();

            int id = UserDataHandler.getNetworkId(player);
            if (id > 0) {
                NetworkClient client = plugin.network().getPlayer(id);
                UserSession session = client.session();

                if (!session.isLogged() && !session.isTotpLogged() && !session.isPinLogged()) {
                    client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                    return false;
                }
                //TODO: Open settings inventory

                OptionsInventory<SettingsButton> settings = UserDataHandler.getSettings(client);
                settings.open(player);
            } else {
                player.sendMessage(Colorize.colorize(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
            }
        }

        return false;
    }
}
