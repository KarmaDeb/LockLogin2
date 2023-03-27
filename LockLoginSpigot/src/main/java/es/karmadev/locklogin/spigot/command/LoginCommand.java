package es.karmadev.locklogin.spigot.command;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import ml.karmaconfigs.api.common.string.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LoginCommand implements CommandExecutor {

    private final LockLogin plugin = CurrentPlugin.getPlugin();

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
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Messages messages = plugin.messages();

            int id = UserDataHandler.getNetworkId(player);
            if (id > 0) {
                NetworkClient client = plugin.network().getPlayer(id);
                UserAccount account = client.account();
                UserSession session = client.session();

                if (account.isRegistered()) {
                    String captcha = StringUtils.stripColor(session.captcha());

                    switch (args.length) {
                        case 1: validate(client, account, args[0]);
                            break;
                        case 2:
                            String inputPassword = args[0];
                            String inputCaptcha = args[1];

                            if (captcha.equals(inputCaptcha)) {
                                validate(client, account, inputPassword);
                            } else {
                                client.sendMessage(messages.prefix() + messages.invalidCaptcha());
                            }
                            break;
                        default:
                            client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                            break;
                    }
                } else {
                    client.sendMessage(messages.prefix() + messages.register(session.captcha()));
                }
            } else {
                player.sendMessage(StringUtils.toColor(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
            }
        } else {
            plugin.info("This command is for players only!");
        }

        return false;
    }

    private void validate(final NetworkClient client, final UserAccount account, final String inputPassword) {
        HashResult hash = account.password();
        Messages messages = plugin.messages();

        if (hash.verify(inputPassword)) {
            client.sendMessage(messages.prefix() + messages.logged());
        } else {
            client.sendMessage(messages.prefix() + messages.incorrectPassword());
        }
    }
}
