package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.security.hash.HashResult;
import es.karmadev.locklogin.api.security.totp.TotpService;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.common.plugin.secure.CommandMask;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import es.karmadev.locklogin.spigot.process.SpigotTotpProcess;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

@PluginCommand(command = "totp", processAttachment = SpigotTotpProcess.class)
public class TotpCommand extends Command {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();

    public TotpCommand(final String cmd) {
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

                if (!account.isRegistered()) {
                    client.sendMessage(messages.prefix() + messages.register(session.captcha()));
                    return false;
                }

                if (args.length >= 1) {
                    String argument = args[0].toLowerCase();
                    switch (argument.toLowerCase()) {
                        case "enable":
                        case "setup": {
                            if (args.length < 2) {
                                client.sendMessage(messages.prefix() + messages.gAuthSetupUsage());
                                return false;
                            }

                            if (!session.isLogged()) {
                                client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                                return false;
                            }

                            String password = args[1];
                            HashResult storedPwd = account.password();

                            if (!storedPwd.verify(password)) {
                                client.sendMessage(messages.prefix() + messages.incorrectPassword());
                                return false;
                            }

                            if (account.totpSet()) {
                                if (!account.hasTotp()) {
                                    /*
                                    If the account has totp, it means it just has been
                                    disabled previously, so instead of generating a new
                                    totp code, we will just re-enable it
                                     */
                                    account.setTotp(true);
                                    client.sendMessage(messages.prefix() + messages.gAuthEnabled());
                                    return false;
                                }

                                client.sendMessage(messages.prefix() + messages.gAuthSetupAlready());
                                return false;
                            }

                            TotpService service = (TotpService) plugin.getService("totp");
                            URL totpURL = service.generateQR(client);

                            if (totpURL == null) {
                                client.sendMessage(messages.prefix() + "&cAn unexpected error occurred, couldn't generate your totp URL");
                                return false;
                            }

                            account.setTotp(true);
                            client.sendMessage(messages.gAuthInstructions());
                            String[] codes = service.scratchCodes(client);
                            if (codes.length > 0) {
                                client.sendMessage(messages.gAuthScratchCodes(
                                        Arrays.asList(codes)
                                ));
                            }

                            TextComponent component = new TextComponent(ColorComponent.parse(messages.gAuthLink()));
                            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7This will redirect you to:\n§b" + totpURL)));
                            component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, totpURL.toString()));
                            player.spigot().sendMessage(component);
                        }
                        break;
                        case "remove": {
                            if (args.length < 2) {
                                client.sendMessage(messages.prefix() + messages.gAuthRemoveUsage());
                                return false;
                            }

                            if (!session.isLogged()) {
                                client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                                return false;
                            }

                            if (args.length >= 3) {
                                String password = args[1];
                                String code = args[2];

                                if (account.totpSet()) {
                                    /*
                                    We won't care if the client has totp enabled or not, we will always
                                    ask the client for the totp code when he requests it to be removed from
                                    the database
                                     */
                                    TotpService service = (TotpService) plugin.getService("totp");
                                    HashResult resultPwd = account.password();

                                    if (!resultPwd.verify(password)) {
                                        client.sendMessage(messages.prefix() + messages.incorrectPassword());
                                        return false;
                                    }

                                    if (!service.validateTotp(code, client)) {
                                        client.sendMessage(messages.prefix() + messages.gAuthIncorrect());
                                        return false;
                                    }

                                    account.setTotp(null); //We remove his totp, entirely
                                    account.setTotp(false); //Then when tell the account that totp is disabled for this account, regardless of the current value

                                    client.sendMessage(messages.prefix() + messages.gAuthDisabled());
                                } else {
                                    client.sendMessage(messages.prefix() + messages.gAuthNotEnabled());
                                }
                            } else {
                                client.sendMessage(messages.prefix() + messages.gAuthRemoveUsage());
                            }
                        }
                        break;
                        case "disable": {
                            if (args.length < 2) {
                                client.sendMessage(messages.prefix() + messages.gAuthDisableUsage());
                                return false;
                            }

                            if (!session.isLogged()) {
                                client.sendMessage(messages.prefix() + messages.login(session.captcha()));
                                return false;
                            }

                            if (args.length >= 3) {
                                String password = args[1];
                                String code = args[2];

                                if (account.hasTotp()) {
                                    TotpService service = (TotpService) plugin.getService("totp");
                                    HashResult resultPwd = account.password();

                                    if (!resultPwd.verify(password)) {
                                        client.sendMessage(messages.prefix() + messages.incorrectPassword());
                                        return false;
                                    }

                                    if (!service.validateTotp(code, client)) {
                                        client.sendMessage(messages.prefix() + messages.gAuthIncorrect());
                                        return false;
                                    }

                                    account.setTotp(false); //Then when tell the account that totp is disabled for this account
                                    client.sendMessage(messages.prefix() + messages.gAuthDisabled());
                                } else {
                                    client.sendMessage(messages.prefix() + messages.gAuthNotEnabled());
                                }
                            } else {
                                client.sendMessage(messages.prefix() + messages.gAuthDisableUsage());
                            }
                        }
                        break;
                        default:
                            if (!session.fetch("totp_logged", false)) {
                                if (account.hasTotp() && account.totpSet()) {
                                    StringBuilder codeBuilder = new StringBuilder();
                                    for (String arg : args) {
                                        codeBuilder.append(arg);
                                    }

                                    String code = codeBuilder.toString();
                                    TotpService service = (TotpService) plugin.getService("totp");

                                    if (service.validateTotp(code, client)) {
                                        plugin.getTotpHandler().trigger(client, true);

                                        session.append(CSessionField.newField(Boolean.class, "totp_logged", true));
                                        client.sendMessage(messages.prefix() + messages.gAuthCorrect());
                                    } else {
                                        plugin.getTotpHandler().trigger(client, false);
                                        client.sendMessage(messages.prefix() + messages.gAuthIncorrect());
                                    }
                                } else {
                                    client.sendMessage(messages.prefix() + messages.gAuthNotEnabled());
                                }
                            } else {
                                client.sendMessage(messages.prefix() + messages.gAuthAlready());
                            }
                            break;
                    }
                } else {
                    client.sendMessage(messages.prefix() + messages.gAuthUsages());
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
}
