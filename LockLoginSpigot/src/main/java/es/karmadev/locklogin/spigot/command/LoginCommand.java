package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.file.section.BruteForceConfiguration;
import es.karmadev.locklogin.api.plugin.file.spawn.SpawnConfiguration;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.security.brute.BruteForceService;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.SessionField;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import es.karmadev.locklogin.spigot.process.SpigotLoginProcess;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.PlayerLocationStorage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@PluginCommand(command = "login", processAttachment = SpigotLoginProcess.class)
@SuppressWarnings("unused")
public class LoginCommand extends Command {

    private final LockLogin plugin = CurrentPlugin.getPlugin();
    private final ConcurrentMap<UUID, Integer> localFailCount = new ConcurrentHashMap<>();

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

                if (session.fetch("pass_logged", false)) {
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
                                SessionField<String> recoveryCode = session.fetch("recovery_code");
                                if (recoveryCode != null) {
                                    if (recoveryCode.get().equals(args[0])) {
                                        client.sendMessage(messages.prefix() + messages.logged());
                                        account.setPassword(null);

                                        client.sendMessage(messages.prefix() + messages.register(""));
                                    } else {
                                        int count = localFailCount.compute(client.uniqueId(), (key, value) -> {
                                            if (value == null) {
                                                return 1;
                                            }

                                            return value + 1;
                                        });

                                        int triesLeft = 5 - count;
                                        client.sendMessage(messages.prefix() + "&5&oInvalid recovery code; " + triesLeft + " tries left");
                                    }

                                    return false;
                                }

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
                player.sendMessage(Colorize.colorize(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
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
            InetSocketAddress address = player.getAddress();
            if (address == null) {
                client.kick(messages.ipProxyError());
                return;
            }

            PluginService service = plugin.getService("bruteforce");
            if (service instanceof BruteForceService) {
                BruteForceService bruteForce = (BruteForceService) service;
                bruteForce.success(address.getAddress());
            }

            if (hash.hasher().isLegacy()) {
                plugin.info("Migrated password from legacy client {0}", client.name());
                account.setPassword(inputPassword); //Update the client password
            }

            session.append(CSessionField.newField(Boolean.class, "pass_logged", true));
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

            SpawnConfiguration spawn = configuration.spawn();
            if (spawn.takeBack()) {
                PlayerLocationStorage storage = new PlayerLocationStorage(client);
                Location location = storage.load();

                if (location != null) {
                    player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
                }
            }
        } else {
            InetSocketAddress address = player.getAddress();
            if (address == null) {
                client.kick(messages.ipProxyError());
                return;
            }

            client.sendMessage(messages.prefix() + messages.incorrectPassword());

            PluginService service = plugin.getService("bruteforce");
            if (service instanceof BruteForceService) {
                BruteForceConfiguration config = configuration.bruteForce();
                BruteForceService bruteForce = (BruteForceService) service;


                int count = localFailCount.compute(client.uniqueId(), (key, value) -> {
                    if (value == null) {
                        return 1;
                    }

                    return value + 1;
                });

                if (count >= config.attempts()) {
                    if (!ObjectUtils.isNullOrEmpty(client.account().email()) && configuration.mailer().isEnabled()) {
                        client.getSessionChecker().pause();

                        client.sendMessage(messages.prefix() + messages.loginForgot());
                        TextComponent yes = new TextComponent(Colorize.colorize(messages.loginForgotYes()));
                        TextComponent no = new TextComponent(Colorize.colorize(messages.loginForgotNo()));
                        TextComponent appender = new TextComponent();
                        appender.addExtra(yes);
                        appender.addExtra(Colorize.colorize(" &8&l| "));
                        appender.addExtra(no);

                        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Colorize.colorize("&bWe will mail you a recovery code"))));
                        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(Colorize.colorize("&cWe won't send you any recovery mail"))));

                        String mailCode = StringUtils.generateString(16);
                        String kickCode = StringUtils.generateString(16);

                        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mailme " + mailCode));
                        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kickme " + kickCode));

                        client.session().append(CSessionField.newField(String.class, "mail_code", mailCode));
                        client.session().append(CSessionField.newField(String.class, "kick_code", kickCode));

                        player.spigot().sendMessage(appender);
                        localFailCount.remove(client.uniqueId());
                        return;
                    }

                    int tries = bruteForce.tries(address.getAddress());
                    if (tries + 1 >= config.tries()) {
                        client.kick(messages.ipBlocked(TimeUnit.MINUTES.toSeconds(config.blockTime())));
                        bruteForce.success(address.getAddress()); //We use this to "clear" tries count
                        bruteForce.block(address.getAddress(), config.blockTime());
                        return;
                    }

                    if (account.isProtected()) {
                        bruteForce.success(address.getAddress()); //We use this to "clear" tries count and allow the "final panic try"
                        client.kick(messages.panicLogin());
                        bruteForce.togglePanic(client, true); //We are now panicking
                        return;
                    }

                    client.kick(messages.incorrectPassword());
                    bruteForce.fail(address.getAddress());
                }
            }
        }
    }
}
