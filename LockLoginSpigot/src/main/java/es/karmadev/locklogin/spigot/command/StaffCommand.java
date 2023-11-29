package es.karmadev.locklogin.spigot.command;

import es.karmadev.api.file.util.PathUtilities;
import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.api.minecraft.uuid.UUIDFetcher;
import es.karmadev.api.minecraft.uuid.UUIDType;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.bundler.TextEntity;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.plugin.service.name.NameValidator;
import es.karmadev.locklogin.api.security.brute.BruteForceService;
import es.karmadev.locklogin.api.security.check.CheckResult;
import es.karmadev.locklogin.api.security.check.PasswordValidator;
import es.karmadev.locklogin.api.user.UserFactory;
import es.karmadev.locklogin.api.user.account.AccountFactory;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.protection.type.SHA512Hash;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.command.helper.PluginCommand;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@PluginCommand(command = "staff") @SuppressWarnings("unused")
public class StaffCommand extends Command {

    private final LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final Set<UUID> staff = ConcurrentHashMap.newKeySet();

    public StaffCommand(final @NotNull String name) {
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
    public boolean execute(final @NotNull CommandSender sender, final @NotNull String label, final @NotNull String[] args) {
        Messages messages = spigot.messages();
        Configuration configuration = spigot.configuration();

        if (sender instanceof Player) {
            Player player = (Player) sender;
            int networkId = UserDataHandler.getNetworkId(player);

            if (networkId <= 0) {
                player.sendMessage(Colorize.colorize(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
                return false;
            }

            NetworkClient issuer = spigot.network().getPlayer(networkId);
            if (issuer == null) {
                player.sendMessage(Colorize.colorize(messages.prefix() + "&cYour session is not valid, reconnect the server!"));
                return false;
            }

            process(TextEntity.singleton(issuer), messages, configuration, args);
        } else {
            process(TextEntity.singleton(spigot), messages, configuration, args);
        }

        return false;
    }

    private void process(final TextEntity<?> issuer, final Messages messages, final Configuration configuration, final String[] args) {
        if (args.length == 0) {
            issuer.getComponent().sendMessage(messages.prefix() + messages.staffUsage());
            return;
        }
        String argument = args[0].toLowerCase();

        if (!hasSetup() && !argument.equals("setup")) {
            if (issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_SETUP)) {
                issuer.getComponent().sendMessage(messages.prefix() + messages.staffSetup());
            } else {
                issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_SETUP));
            }

            return;
        }
        if (!issuer.getComponent().equals(spigot) && !staff.contains(issuer.getComponent().uniqueId()) && !argument.equalsIgnoreCase("toggle")) {
            issuer.getComponent().sendMessage(messages.prefix() + messages.staffToggleUsage());
            return;
        }

        switch (argument) {
            case "toggle":
                if (issuer.getComponent().equals(spigot)) {
                    spigot.err("You cannot toggle yourself!");
                    return;
                }

                if (!issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_TOGGLE)) {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_TOGGLE));
                    return;
                }

                String key = args[1];
                if (validateSetup(key)) {
                    if (staff.contains(issuer.getComponent().uniqueId())) {
                        staff.remove(issuer.getComponent().uniqueId());
                    } else {
                        staff.add(issuer.getComponent().uniqueId());
                    }

                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffToggleSuccess());
                    return;
                }

                issuer.getComponent().sendMessage(messages.prefix() + messages.staffToggleFailure());
                break;
            case "setup":
                if (!issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_SETUP)) {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_SETUP));
                    return;
                }

                switch (args.length) {
                    case 2:
                        if (hasSetup()) {
                            issuer.getComponent().sendMessage(messages.prefix() + messages.staffSetup());
                            return;
                        }

                        makeSetup(args[1]);
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffSetupSuccess());
                        break;
                    case 3:
                        if (!hasSetup()) {
                            issuer.getComponent().sendMessage(messages.prefix() + messages.staffSetup());
                            return;
                        }

                        if (validateSetup(args[1])) {
                            makeSetup(args[2]);
                            issuer.getComponent().sendMessage(messages.prefix() + messages.staffSetupSuccess());

                            staff.removeIf((id) -> !issuer.getComponent().uniqueId().equals(id));
                            return;
                        }

                        issuer.getComponent().sendMessage("&5&oINVALID STAFF KEY");
                        break;
                    default:
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffSetup());
                        break;
                }
                break;
            case "register":
                if (!issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_REGISTER)) {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_REGISTER));
                    return;
                }

                if (args.length == 3) {
                    String username = args[1];
                    String password = args[2];

                    if (!validateName(username, messages)) {
                        issuer.getComponent().sendMessage(messages.prefix() + "&5&oFailed to validate username");
                        return;
                    }

                    UUID offlineId = UUIDFetcher.fetchUUID(username, UUIDType.OFFLINE);
                    LocalNetworkClient client = spigot.network().getOfflinePlayer(offlineId);
                    if (client == null) {
                        UserFactory<? extends LocalNetworkClient> factory = spigot.getUserFactory(false);
                        client = factory.create(username, offlineId);
                    }

                    /*if (client.online() || client.client() != null) {
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffRegisterFailure(username));
                        issuer.getComponent().sendMessage(messages.prefix() + "&5&oClient is online");
                        return;
                    }*/

                    if (!validPassword(username, password, client, messages, configuration)) {
                        issuer.getComponent().sendMessage(messages.prefix() + "&5&oPassword is not strong enough");
                        return;
                    }

                    UserAccount account = client.account();
                    if (account.id() <= 0) {
                        client.reset("account");

                        AccountFactory<? extends UserAccount> factory = spigot.getAccountFactory(false);
                        account = factory.create(client);

                        spigot.info("Created account with id {0} for user {1}", account.id(), client.name());
                        client.account(); //Cache
                    }

                    account.setPassword(password);
                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffRegisterSuccess(client, password));

                    for (NetworkClient user : spigot.network().getOnlinePlayers()) {
                        if (user.id() != issuer.getComponent().id() && user.hasPermission(LockLoginPermission.PERMISSION_STAFF_ECHO)) {
                            user.sendMessage(messages.prefix() + messages.staffEcho(spigot, "ACCOUNT_CREATE", client));
                        }
                    }
                } else {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffRegisterUsage());
                }
                break;
            case "login":
                if (!issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_LOGIN)) {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_LOGIN));
                    return;
                }

                if (args.length == 2) {
                    String username = args[1];

                    LocalNetworkClient client = spigot.network().getPlayer(username);
                    if (client == null || !client.online()) {
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffLoginFailure(username));
                        return;
                    }

                    NetworkClient online = client.client();
                    assert online != null;

                    UserAccount account = online.account();
                    if (!account.isRegistered()) {
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffLoginFailure(username));
                        return;
                    }

                    UserSession session = online.session();
                    boolean changes = false;
                    if (!session.isValid()) {
                        session.validate();
                        changes = true;
                    }

                    session.append(CSessionField.newField(Boolean.class, "pass_logged", true));
                    session.append(CSessionField.newField(Boolean.class, "pin_logged", true));
                    session.append(CSessionField.newField(Boolean.class, "totp_logged", true));

                    if (!session.isLogged()) {
                        session.login(true);
                        changes = true;
                    }
                    if (!session.isPinLogged()) {
                        session.pinLogin(true);
                        changes = true;
                    }
                    if (!session.isTotpLogged()) {
                        session.totpLogin(true);
                        changes = true;
                    }
                    if (!session.isCaptchaLogged()) {
                        session.captchaLogin(true);
                        changes = true;
                    }

                    if (changes) {
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffLoginSuccess(online));
                        online.sendMessage(messages.prefix() + messages.logged());

                        for (NetworkClient user : spigot.network().getOnlinePlayers()) {
                            if (user.id() != issuer.getComponent().id() && user.hasPermission(LockLoginPermission.PERMISSION_STAFF_ECHO)) {
                                user.sendMessage(messages.prefix() + messages.staffEcho(spigot, "LOGIN_IN", online));
                            }
                        }

                        return;
                    }

                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffLoginFailure(username));
                } else {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffLoginUsage());
                }
                break;
            case "logout":
                if (!issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_LOGOUT)) {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_LOGOUT));
                    return;
                }

                if (args.length == 2) {
                    String username = args[1];

                    LocalNetworkClient client = spigot.network().getPlayer(username);
                    if (client == null || !client.online()) {
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffLogoutFailure(username));
                        return;
                    }

                    NetworkClient online = client.client();
                    assert online != null;

                    UserSession session = online.session();
                    boolean changes = false;

                    session.append(CSessionField.newField(Boolean.class, "pass_logged", false));
                    session.append(CSessionField.newField(Boolean.class, "pin_logged", false));
                    session.append(CSessionField.newField(Boolean.class, "totp_logged", false));

                    if (session.isLogged()) {
                        session.login(false);
                        changes = true;
                    }
                    if (session.isPinLogged()) {
                        session.pinLogin(false);
                        changes = true;
                    }
                    if (session.isTotpLogged()) {
                        session.totpLogin(false);
                        changes = true;
                    }
                    if (session.isCaptchaLogged()) {
                        session.captchaLogin(false);
                        changes = true;
                    }

                    if (changes) {
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffLogoutSuccess(online));
                        online.sendMessage(messages.prefix() + messages.sessionClosed());

                        for (NetworkClient user : spigot.network().getOnlinePlayers()) {
                            if (user.id() != issuer.getComponent().id() && user.hasPermission(LockLoginPermission.PERMISSION_STAFF_ECHO)) {
                                user.sendMessage(messages.prefix() + messages.staffEcho(spigot, "LOGIN_OUT", online));
                            }
                        }

                        return;
                    }

                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffLogoutFailure(username));
                } else {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffLogoutUsage());
                }
                break;
            case "unregister":
                if (!issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_UNREGISTER)) {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_UNREGISTER));
                    return;
                }

                if (args.length == 2) {
                    String username = args[1];

                    LocalNetworkClient client = spigot.network().getPlayer(username);
                    UserAccount account = client.account();
                    if (account.id() <= 0 || !account.isRegistered()) {
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffUnregisterFailure(username));
                        return;
                    }

                    account.setPassword(null);
                    account.setPin(null);
                    account.setTotp(null);
                    account.setPanic(null);
                    account.setTotp(false);
                    account.reset();

                    UserSession session = client.session();

                    session.append(CSessionField.newField(Boolean.class, "pass_logged", false));
                    session.append(CSessionField.newField(Boolean.class, "pin_logged", false));
                    session.append(CSessionField.newField(Boolean.class, "totp_logged", false));

                    session.login(false);
                    session.pinLogin(false);
                    session.totpLogin(false);
                    session.captchaLogin(false);

                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffUnregisterSuccess(client));
                    NetworkClient online = client.client();
                    if (client.online()) {
                        assert online != null;
                        online.sendMessage(messages.prefix() + messages.accountTerminated());
                    }

                    for (NetworkClient user : spigot.network().getOnlinePlayers()) {
                        if (user.id() != issuer.getComponent().id() && user.hasPermission(LockLoginPermission.PERMISSION_STAFF_ECHO)) {
                            user.sendMessage(messages.prefix() + messages.staffEcho(spigot, "ACCOUNT_TERMINATE", online));
                        }
                    }
                } else {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffUnregisterUsage());
                }
                break;
            case "lookup":
                if (!issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_LOOKUP)) {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_LOOKUP));
                    return;
                }

                if (args.length == 2) {
                    String filter = args[1];

                    Collection<LocalNetworkClient> clients = spigot.network().getPlayers().stream().filter((cl) -> {
                        UUID uniqueId = cl.uniqueId();
                        UUID onlineId = cl.onlineId();
                        String name = cl.name();
                        String email = cl.account().email();
                        InetSocketAddress address = cl.address();

                        if (onlineId == null) onlineId = uniqueId;

                        String host = address.getHostName();
                        String rawUid = uniqueId.toString().replaceAll("-", "");
                        String rawPUid = onlineId.toString().replaceAll("-", "");

                        if (email == null) email = "";

                        return name.equalsIgnoreCase(filter) || rawUid.equalsIgnoreCase(filter.replaceAll("-", "")) ||
                                rawPUid.equalsIgnoreCase(filter.replaceAll("-", "")) || email.startsWith(filter) ||
                                host.equalsIgnoreCase(filter);
                    }).collect(Collectors.toList());

                    if (clients.isEmpty()) {
                        issuer.getComponent().sendMessage(messages.prefix() + messages.staffLookupEmpty(filter));
                        return;
                    }

                    for (LocalNetworkClient client : clients) {
                        UUID uniqueId = client.uniqueId();
                        UUID onlineId = client.onlineId();
                        String name = client.name();
                        String email = client.account().email();
                        InetSocketAddress address = client.address();

                        if (onlineId == null) onlineId = uniqueId;

                        String host = address.getHostName();
                        String rawUid = uniqueId.toString().replaceAll("-", "");
                        String rawPUid = onlineId.toString().replaceAll("-", "");

                        if (email == null) email = "";

                        issuer.getComponent().sendMessage("&0&m-----------------------------");
                        issuer.getComponent().sendMessage("");
                        issuer.getComponent().sendMessage("&dID: &e" + client.id());
                        issuer.getComponent().sendMessage("&dClient: &e" + bold(name, filter));
                        issuer.getComponent().sendMessage("&dUUID: &e" + bold(rawUid, filter.replace("-", "")));
                        issuer.getComponent().sendMessage("&dOnline UUID: &e" +  bold(rawPUid, filter.replace("-", "")));
                        issuer.getComponent().sendMessage("&dEmail: &e" + bold(email, filter));
                        issuer.getComponent().sendMessage("&dIP address: &e" + bold(host, filter));
                    }
                } else {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffLookupUsage());
                }

                break;
            case "unban":
                if (!issuer.getComponent().hasPermission(LockLoginPermission.PERMISSION_STAFF_UNBAN)) {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.permissionError(LockLoginPermission.PERMISSION_STAFF_UNBAN));
                    return;
                }

                if (args.length == 2) {
                    String rawAddress = args[1];
                    try {
                        InetAddress address = InetAddress.getByName(rawAddress);

                        PluginService bf_service = spigot.getService("bruteforce");
                        if (bf_service instanceof BruteForceService) {
                            BruteForceService bruteforce = (BruteForceService) bf_service;

                            if (!bruteforce.isBlocked(address)) {
                                issuer.getComponent().sendMessage(messages.prefix() + messages.staffUnbanFailure(address));
                                return;
                            }

                            bruteforce.unblock(address);
                            issuer.getComponent().sendMessage(messages.prefix() + messages.staffUnbanSuccess(address));
                        }
                    } catch (UnknownHostException ex) {
                        issuer.getComponent().sendMessage(messages.prefix() + "&5&oINVALID IP ADDRESS PROVIDED");
                    }

                    return;
                } else {
                    issuer.getComponent().sendMessage(messages.prefix() + messages.staffUnbanUsage());
                }
                break;
            default:
                issuer.getComponent().sendMessage(messages.prefix() + messages.staffUsage());
                break;
        }
    }

    private boolean hasSetup() {
        Path authData = spigot.workingDirectory().resolve("cache").resolve("staff");
        return Files.exists(authData);
    }

    private boolean validateSetup(final String password) {
        Path authData = spigot.workingDirectory().resolve("cache").resolve("staff");
        if (!Files.exists(authData)) return false;

        String content = PathUtilities.read(authData);
        SHA512Hash sha = new SHA512Hash();

        return sha.auth(password, content);
    }

    private void makeSetup(final String password) {
        Path authData = spigot.workingDirectory().resolve("cache").resolve("staff");
        SHA512Hash sha = new SHA512Hash();

        PathUtilities.write(authData, sha.hashInput(password));
    }

    private String bold(final String input, final String match) {
        int indexStart = -1;
        int indexEnd = -1;

        final int matchLength = match.length();
        final int maxLength = input.length();

        for (int i = 0; i < matchLength; i++) {
            if (i + matchLength < maxLength) {
                String subPart = input.substring(i, i + matchLength);
                if (subPart.equalsIgnoreCase(match)) {
                    indexStart = i;
                    indexEnd = i + matchLength;
                    break;
                }
            }
        }

        if (indexStart > -1 && indexEnd > -1) {
            String part = input.substring(indexStart, indexEnd);
            return input.replace(part, "&l" + part + "&r&e");
        }

        return input;
    }

    private boolean validateName(final String username, final Messages messages) {
        PluginService name_service = spigot.getService("name");
        if (name_service instanceof ServiceProvider) {
            ServiceProvider<? extends PluginService> provider = (ServiceProvider<?>) name_service;
            PluginService service = provider.serve(username);

            if (service instanceof NameValidator) {
                NameValidator validator = (NameValidator) service;
                validator.validate();

                if (!validator.isValid()) {
                    spigot.err(messages.illegalName(validator.invalidCharacters()));
                    spigot.info(messages.staffRegisterFailure(username));
                    return false;
                }
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean validPassword(final String username, final String password, final LocalNetworkClient client, final Messages messages, final Configuration configuration) {
        PluginService service = spigot.getService("password");
        ServiceProvider<PasswordValidator> provider = null;
        if (service instanceof ServiceProvider) {
            ServiceProvider<?> unknownProvider = (ServiceProvider<?>) service;
            if (unknownProvider.getService().isAssignableFrom(PasswordValidator.class)) {
                provider = (ServiceProvider<PasswordValidator>) unknownProvider;
            }
        }

        if (provider == null) throw new IllegalStateException("Cannot create account with invalid password validator");
        PasswordValidator validator = provider.serve(password);
        CheckResult result = validator.validate();

        String message = messages.checkResult(result);
        if (!result.valid()) {
            if (configuration.password().blockUnsafe()) {
                spigot.err(message);
                spigot.info(messages.staffRegisterFailure(username));
                return false;
            }

            if (configuration.password().warningUnsafe()) {
                for (NetworkClient online : spigot.network().getOnlinePlayers()) {
                    if (online.id() != client.id() && online.hasPermission(LockLoginPermission.PERMISSION_UNSAFE_WARNING)) {
                        online.sendMessage(messages.prefix() + messages.passwordWarning(client));
                    }
                }

                spigot.warn(messages.passwordInsecure());
            }
        }

        return true;
    }
}
