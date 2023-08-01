package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.file.section.PasswordConfiguration;
import es.karmadev.locklogin.api.plugin.file.section.SpawnSection;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.security.check.CheckResult;
import es.karmadev.locklogin.api.security.check.PasswordValidator;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.PlayerLocationStorage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RegisterCommand implements CommandExecutor {

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
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, @NotNull String[] args) {
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
                if (account.id() <= 0) {
                    AccountFactory<? extends UserAccount> factory = plugin.getAccountFactory(false);
                    account = factory.create(client);

                    plugin.info("Created account with id {0} for user {1}", account.id(), client.name());
                }

                UserSession session = client.session();

                if (account.isRegistered()) {
                    if (session.isLogged()) {
                        client.sendMessage(messages.prefix() + messages.alreadyRegistered());
                        return false;
                    }
                } else {
                    String captcha = session.captcha();
                    if (captcha == null) captcha = "";
                    captcha = ConsoleColor.strip(captcha);

                    switch (args.length) {
                        case 1:
                            client.sendMessage(messages.prefix() + messages.register(captcha));
                            break;
                        case 2:
                            if (captcha.isEmpty()) {
                                String passwordInput = args[0];
                                String passwordConfirmation = args[1];

                                register(player, client, account, session, passwordInput, passwordConfirmation);
                            } else {
                                client.sendMessage(messages.prefix() + messages.invalidCaptcha());
                            }
                            break;
                        case 3:
                            String passwordInput = args[0];
                            String passwordConfirmation = args[1];
                            String captchaCode = args[2];

                            if (captcha.isEmpty() || captchaCode.equals(captcha)) {
                                session.setCaptcha(null);
                                register(player, client, account, session, passwordInput, passwordConfirmation);
                            } else {
                                client.sendMessage(messages.prefix() + messages.invalidCaptcha());
                            }
                            break;
                        default:
                            client.sendMessage(messages.prefix() + messages.register(session.captcha()));
                            break;
                    }

                    //client.sendMessage(messages.prefix() + messages.register(session.captcha()));
                }
            } else {
                player.sendMessage(ColorComponent.parse(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
            }
        } else {
            plugin.info("This command is for players only!");
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private void register(final Player player, final NetworkClient client, final UserAccount account, final UserSession session, final String passwordInput, final String passwordConfirmation) {
        Messages messages = plugin.messages();
        Configuration configuration = plugin.configuration();
        PasswordConfiguration passwordConfiguration = configuration.password();

        PluginService service = plugin.getService("password");
        ServiceProvider<PasswordValidator> provider = null;
        if (service instanceof ServiceProvider) {
            ServiceProvider<?> unknownProvider = (ServiceProvider<?>) service;
            if (unknownProvider.getService().isAssignableFrom(PasswordValidator.class)) {
                provider = (ServiceProvider<PasswordValidator>) unknownProvider;
            }
        }

        if (passwordInput.equals(passwordConfirmation)) {
            if (provider != null) {
                PasswordValidator validator = provider.serve(passwordInput);
                CheckResult result = validator.validate();

                String message = messages.checkResult(result);
                if (!result.valid()) {
                    if (passwordConfiguration.blockUnsafe()) {
                        client.sendMessage(message);
                        return;
                    }

                    if (passwordConfiguration.warningUnsafe()) {
                        for (NetworkClient online : plugin.network().getOnlinePlayers()) {
                            if (online.id() != client.id() && online.hasPermission(LockLoginPermission.PERMISSION_UNSAFE_WARNING)) {
                                online.sendMessage(messages.prefix() + messages.passwordWarning(client));
                            }
                        }

                        client.sendMessage(messages.prefix() + messages.passwordInsecure());
                    }
                } else {
                    if (passwordConfiguration.printSuccess()) {
                        client.sendMessage(message);
                    }
                }
            }

            account.setPassword(passwordInput);
            client.sendMessage(messages.prefix() + messages.registered());

            /*session.login(true);
            session._2faLogin(true);
            session.pinLogin(true);*/
            session.append(CSessionField.newField(Boolean.class, "logged", true));

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
            client.sendMessage(messages.prefix() + messages.registerError());
        }
    }
}
