package es.karmadev.locklogin.spigot.command;

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
import es.karmadev.locklogin.spigot.process.SpigotPinProcess;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.PlayerLocationStorage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@PluginCommand(command = "pin", processAttachment = SpigotPinProcess.class)
public class PinCommand extends Command {

    private final LockLogin plugin = CurrentPlugin.getPlugin();

    public PinCommand(final String cmd) {
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
                //return false;
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

                if (session.isLogged()) {
                    if (args.length >= 2) {
                        String argument = args[0].toLowerCase();
                        switch (argument.toLowerCase()) {
                            case "setup": {
                                if (account.hasPin()) {
                                    client.sendMessage(messages.prefix() + messages.alreadyPin());
                                    return false;
                                }

                                String rawPin = args[1].toLowerCase();
                                if (rawPin.length() != 4) {
                                    client.sendMessage(messages.prefix() + messages.pinLength());
                                    return false;
                                }

                                try {
                                    Integer.parseInt(rawPin);
                                    /*
                                    We just want to validate if the user has put a valid number, we later use the same string
                                    argument because a user might add dealing zeros to its pin, which in java, there's no way
                                    to have them in an "int" variable
                                     */

                                    account.setPin(rawPin);
                                    client.sendMessage(messages.prefix() + messages.pinSet());
                                } catch (NumberFormatException ex) {
                                    client.sendMessage(messages.prefix() + messages.pinLength());
                                    return false;
                                }
                            }
                                break;
                            case "remove": {
                                if (!account.hasPin()) {
                                    client.sendMessage(messages.prefix() + messages.noPin());
                                    return false;
                                }

                                String rawPin = args[1].toLowerCase();
                                if (rawPin.length() != 4) {
                                    client.sendMessage(messages.prefix() + messages.pinLength());
                                    return false;
                                }

                                try {
                                    Integer.parseInt(rawPin);
                                    /*
                                    We just want to validate if the user has put a valid number, we later use the same string
                                    argument because a user might add dealing zeros to its pin, which in java, there's no way
                                    to have them in an "int" variable
                                     */

                                    HashResult pin = account.pin();
                                    if (pin.verify(rawPin)) {
                                        account.setPin(null);
                                        client.sendMessage(messages.prefix() + messages.pinReseted());
                                    } else {
                                        client.sendMessage(messages.prefix() + messages.incorrectPin());
                                    }
                                } catch (NumberFormatException ex) {
                                    client.sendMessage(messages.prefix() + messages.pinLength());
                                    return false;
                                }
                            }
                                break;
                            case "change":
                                if (args.length >= 3) {
                                    if (!account.hasPin()) {
                                        client.sendMessage(messages.prefix() + messages.noPin());
                                        return false;
                                    }

                                    String rawPin = args[1].toLowerCase();
                                    String newPin = args[2].toLowerCase();
                                    if (rawPin.length() != 4 || newPin.length() != 4) {
                                        client.sendMessage(messages.prefix() + messages.pinLength());
                                        return false;
                                    }

                                    try {
                                        Integer.parseInt(rawPin);
                                        Integer.parseInt(newPin);
                                        /*
                                        We just want to validate if the user has put a valid number, we later use the same string
                                        argument because a user might add dealing zeros to its pin, which in java, there's no way
                                        to have them in an "int" variable
                                         */

                                        HashResult pin = account.pin();
                                        if (pin.verify(rawPin)) {
                                            account.setPin(newPin);
                                            client.sendMessage(messages.prefix() + messages.pinChanged());
                                        } else {
                                            client.sendMessage(messages.prefix() + messages.incorrectPin());
                                        }
                                    } catch (NumberFormatException ex) {
                                        client.sendMessage(messages.prefix() + messages.pinLength());
                                        return false;
                                    }
                                } else {
                                    client.sendMessage(messages.prefix() + messages.pinUsages());
                                }
                                break;
                            default:
                                client.sendMessage(messages.prefix() + messages.pinUsages());
                                break;
                        }
                    } else {
                        client.sendMessage(messages.prefix() + messages.pinUsages());
                    }
                } else {
                    if (account.isRegistered()) {
                        client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                    } else {
                        client.sendMessage(messages.prefix() + messages.register(session.captcha()));
                    }
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
