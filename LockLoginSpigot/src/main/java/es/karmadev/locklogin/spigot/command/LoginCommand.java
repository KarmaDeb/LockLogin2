package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.file.section.SpawnSection;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.PlayerLocationStorage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@PluginCommand(command = "login")
public class LoginCommand extends Command {

    private final LockLogin plugin = CurrentPlugin.getPlugin();

    public LoginCommand(final String cmd) {
        super(cmd);
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
    public boolean execute(final @NotNull CommandSender sender, final @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            String last_argument = args[args.length - 1];
            try {
                UUID commandId = UUID.fromString(last_argument);
                args = CommandMask.getArguments(commandId);
                CommandMask.consume(commandId);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Messages messages = plugin.messages();

            int id = UserDataHandler.getNetworkId(player);
            if (id > 0) {
                NetworkClient client = plugin.network().getPlayer(id);
                UserAccount account = client.account();
                UserSession session = client.session();

                if (session.fetch("logged", false)) {
                    client.sendMessage(messages.prefix() + messages.alreadyLogged());
                    return false;
                }

                if (account.isRegistered()) {
                    String captcha = session.captcha();
                    if (captcha == null) captcha = "";
                    captcha = ConsoleColor.strip(captcha);

                    switch (args.length) {
                        case 1:
                            if (captcha.isEmpty()) {
                                validate(player, client, account, session, args[0]);
                            } else {
                                client.sendMessage(messages.prefix() + messages.login(captcha));
                            }
                            break;
                        case 2:
                            String inputPassword = args[0];
                            String inputCaptcha = args[1];

                            if (captcha.isEmpty() || captcha.equals(inputCaptcha)) {
                                validate(player, client, account, session, inputPassword);
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
                //Unlike in LockLogin legacy v2, this invalid-session message is real, and LockLogin cannot proceed without a valid session
                player.sendMessage(ColorComponent.parse(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
            }
        } else {
            plugin.info("This command is for players only!");
        }

        return false;
    }

    private void validate(final Player player, final NetworkClient client, final UserAccount account, UserSession session, final String inputPassword) {
        HashResult hash = account.password();
        Messages messages = plugin.messages();
        Configuration configuration = plugin.configuration();

        if (hash.verify(inputPassword)) {
            if (hash.hasher().isLegacy()) {
                plugin.info("Migrated password from legacy client {0}", client.name());
                account.setPassword(inputPassword); //Update the client password
            }

            session.append(CSessionField.newField(Boolean.class, "logged", true));

            /*session.login(true);
            session._2faLogin(true);
            session.pinLogin(true);*/
            client.sendMessage(messages.prefix() + messages.logged());

            if (player.hasMetadata("walkSpeed")) {
                float walkSpeed = player.getMetadata("walkSpeed").get(0).asFloat();
                player.setWalkSpeed(walkSpeed);

                player.removeMetadata("walkSpeed", (Plugin) plugin.plugin());
            }
            if (player.hasMetadata("flySpeed")) {
                float flySpeed = player.getMetadata("flySpeed").get(0).asFloat();
                player.setFlySpeed(flySpeed);

                player.removeMetadata("flySpeed", (Plugin) plugin.plugin());
            }

            SpawnSection spawn = configuration.spawn();
            if (spawn.takeBack()) {
                PlayerLocationStorage storage = new PlayerLocationStorage(client);
                Location location = storage.load();

                if (location != null) {
                    player.teleport(location);
                }
            }
        } else {
            client.sendMessage(messages.prefix() + messages.incorrectPassword());
        }
    }
}
