package es.karmadev.locklogin.spigot.event;

import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.minecraft.text.Colorize;
import es.karmadev.api.minecraft.text.component.Component;
import es.karmadev.api.minecraft.text.component.title.Times;
import es.karmadev.api.minecraft.uuid.UUIDFetcher;
import es.karmadev.api.minecraft.uuid.UUIDType;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.spigot.reflection.packet.MessagePacket;
import es.karmadev.api.strings.ListSpacer;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.event.entity.client.*;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.language.Messages;
import es.karmadev.locklogin.api.plugin.file.section.PremiumConfiguration;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.task.FutureTask;
import es.karmadev.locklogin.api.user.auth.ProcessFactory;
import es.karmadev.locklogin.api.user.auth.process.UserAuthProcess;
import es.karmadev.locklogin.api.user.auth.process.response.AuthProcess;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.api.user.session.service.SessionCache;
import es.karmadev.locklogin.api.user.session.service.SessionStoreService;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.client.COnlineClient;
import es.karmadev.locklogin.common.api.user.storage.session.CSessionField;
import es.karmadev.locklogin.common.plugin.internal.PluginPermissionManager;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.event.helper.EventHelper;
import es.karmadev.locklogin.spigot.process.SpigotLoginProcess;
import es.karmadev.locklogin.spigot.process.SpigotPinProcess;
import es.karmadev.locklogin.spigot.process.SpigotRegisterProcess;
import es.karmadev.locklogin.spigot.protocol.ProtocolAssistant;
import es.karmadev.locklogin.spigot.util.UserDataHandler;
import es.karmadev.locklogin.spigot.util.storage.SpawnLocationStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.metadata.FixedMetadataValue;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class JoinHandler implements Listener {

    private final LockLoginSpigot plugin;
    private final Configuration configuration;

    private final Set<UUID> passedProcess = ConcurrentHashMap.newKeySet();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    public JoinHandler(final LockLoginSpigot spigot) {
        this.plugin = spigot;
        configuration = plugin.configuration();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (plugin.getRuntime().booting()) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ConsoleColor.parse("&cThe server is booting!"));
            return;
        }

        Messages messages = plugin.messages();
        String name = e.getName();
        PremiumDataStore premium = CurrentPlugin.getPlugin().premiumStore();

        UUID provided_id = e.getUniqueId();
        UUID offline_uid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        UUID online_uid = premium.onlineId(name);
        if (online_uid == null) {
            online_uid = UUIDFetcher.fetchUUID(name, UUIDType.ONLINE);
        }

        LocalNetworkClient offline = plugin.network().getOfflinePlayer(offline_uid);
        if (offline == null) {
            if (online_uid != null) {
                LocalNetworkClient premiumClient = plugin.network().getOfflinePlayer(online_uid);
                if (premiumClient != null) {
                    premiumClient.setUniqueId(offline_uid);
                    offline = premiumClient;
                }
            }

            if (offline == null) {
                offline = plugin.getUserFactory(false).create(name, offline_uid);
                EntityCreatedEvent event = new EntityCreatedEvent(offline);
                plugin.moduleManager().fireEvent(event);
            }
        }

        UserSession session = offline.session();
        validateUser(e, offline, offline_uid, online_uid);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLogin(final PlayerLoginEvent e) {
        Player player = e.getPlayer();
        Messages messages = plugin.messages();

        UserDataHandler.setReady(player);

        UUID id = player.getUniqueId();
        LocalNetworkClient offline = plugin.network().getOfflinePlayer(id);
        NetworkClient onlineClient = offline.client();
        if (!(onlineClient instanceof COnlineClient)) {
            plugin.logWarn("Not configuring client {0} because it is provided by an external service", id);
            return;
        }

        COnlineClient online = ((COnlineClient) onlineClient)
                .onMessageRequest((msg) -> {
                    if (!player.isOnline()) return;
                    player.sendMessage(Colorize.colorize(msg)
                            .replace("{player}", player.getName())
                            .replace("{server}", configuration.server())
                            .replace("{ServerName}", configuration.server()));
                })
                .onActionBarRequest((msg) -> {
                    if (!player.isOnline()) return;

                    Component[] component = Component.builder()
                            .actionbar(msg.replace("{player}", player.getName()
                                            .replace("{server}", configuration.server())
                                            .replace("{ServerName}", configuration.server()))).build();
                    MessagePacket message = new MessagePacket(component);
                    message.send(player);
                })
                .onTitleRequest((msg) -> {
                    if (!player.isOnline()) return;

                    Component[] components = Component.builder()
                            .fadeIn(Times.exact(msg.fadeIn()))
                            .show(Times.exact(msg.show()))
                            .fadeOut(Times.exact(msg.fadeOut()))
                            .title(msg.title()
                                    .replace("{player}", player.getName())
                                    .replace("{server}", configuration.server()))
                            .subtitle(msg.subtitle()
                                    .replace("{player}", player.getName())
                                    .replace("{server}", configuration.server()))
                            .build();

                    MessagePacket message = new MessagePacket(components);
                    message.send(player);
                })
                .onKickRequest((msg) -> {
                    if (!player.isOnline()) return;

                    List<String> reasons = Arrays.asList(msg);
                    String reason = StringUtils.listToString(Colorize.colorize(reasons, ArrayList::new),
                                    ListSpacer.NEW_LINE).replace("{player}", player.getName())
                            .replace("{server}", configuration.server())
                            .replace("{ServerName}", configuration.server());

                    Bukkit.getScheduler().runTask(plugin.plugin(), () -> player.kickPlayer(reason));
                })
                .onCommandRequest((command) -> {
                    if (!player.isOnline()) return;

                    if (!command.startsWith("/")) command = "/" + command;
                    PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, command); //Completely emulate the command process
                    Bukkit.getServer().getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        player.performCommand(event.getMessage());
                    }
                })
                .onPermissionRequest((permission) -> {
                    if (!player.isOnline()) return false;
                    if (permission.equalsIgnoreCase("op")) return player.isOp();

                    return player.hasPermission(permission);
                });

        CPluginNetwork network = (CPluginNetwork) plugin.network();
        network.appendClient(online);

        boolean auth = plugin.bungeeMode();
        //TODO: Logic to auto-login if needed

        online.session().append(CSessionField.newField(Boolean.class, "pass_logged", auth));
        online.session().append(CSessionField.newField(Boolean.class, "pin_logged", auth));
        online.session().append(CSessionField.newField(Boolean.class, "totp_logged", auth));
        if (auth) {
            online.session().login(true);
            online.session().pinLogin(true);
            online.session().totpLogin(true);
        }

        player.setMetadata("networkId", new FixedMetadataValue(plugin.plugin(), online.id()));

        if (configuration.spawn().enabled()) {
            Location spawn = SpawnLocationStorage.load();
            if (spawn != null) {
                player.teleport(spawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
        }
        passedProcess.remove(online.uniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPostJoin(final PlayerJoinEvent e) {
        Player player = e.getPlayer();

        NetworkClient online = plugin.network().getPlayer(UserDataHandler.getNetworkId(player));
        Messages messages = plugin.messages();

        online.setAddress(player.getAddress());
        if (configuration.clearChat()) {
            ProtocolAssistant.clearChat(player);
        }

        UserSession session = online.session();
        session.reset("pass_logged");
        session.reset("pin_logged");
        session.reset("totp_logged");

        if (session.isPersistent()) {
            PluginService sessionStoreProvider = plugin.getService("persistence");
            if (sessionStoreProvider instanceof ServiceProvider) {
                ServiceProvider<SessionStoreService> provider = (ServiceProvider<SessionStoreService>) sessionStoreProvider;
                SessionStoreService service = provider.serve(plugin.driver());

                if (service != null) {
                    SessionCache cache = service.getSession(player.getAddress());
                    if (cache != null && cache.getClient().id() == online.id() && cache.getAddress().equals(online.address())) {
                        session.login(cache.isLogged());
                        session.pinLogin(cache.isPinLogged());
                        session.totpLogin(cache.isTotpLogged());

                        session.append(CSessionField.newField(Boolean.class, "pass_logged", true));
                        session.append(CSessionField.newField(Boolean.class, "pin_logged", true));
                        session.append(CSessionField.newField(Boolean.class, "totp_logged", true));

                        online.sendMessage(messages.prefix() + messages.logged());
                    }
                }
            }
        }

        PluginPermissionManager<OfflinePlayer, String> manager = plugin.getPermissionManager();
        if (manager != null) {
            if (!session.isLogged())
                manager.removeAllPermission(player);

            manager.applyGrants(player);
        }

        if (!session.isLogged())
            startAuthProcess(online, null);

        if (online.hasPermission(LockLoginPermission.PERMISSION_JOIN_SILENT)) return;
        String customMessage = messages.join(online);
        String joinMessage = e.getJoinMessage();

        if (!ObjectUtils.isNullOrEmpty(customMessage)) {
            Bukkit.broadcastMessage(Colorize.colorize(customMessage));
        } else {
            if (!ObjectUtils.isNullOrEmpty(joinMessage)) {
                Bukkit.broadcastMessage(Colorize.colorize(joinMessage));
            }
        }
    }

    private boolean invalidIP(final InetAddress address) {
        if (configuration.verifyIpAddress()) {
            if (ObjectUtils.isNullOrEmpty(address)) {
                return true;
            }

            Matcher matcher = IPv4_PATTERN.matcher(address.getHostAddress());
            return !matcher.matches();
        }

        return false;
    }

    public void resetProcess(final NetworkClient client) {
        passedProcess.remove(client.uniqueId());

        ProcessFactory factory = plugin.getProcessFactory();
        factory.reset(client);
    }

    public void startAuthProcess(final NetworkClient client, final AuthProcess previous) {
        ProcessFactory factory = plugin.getProcessFactory();
        UserAuthProcess process = factory.nextProcess(client).orElse(null);
        if (process == null && !passedProcess.contains(client.uniqueId())) {
            EntityAuthenticateEvent event = new EntityAuthenticateEvent(client, true);
            plugin.moduleManager().fireEvent(event);

            if (event.isCancelled()) {
                Bukkit.getScheduler().runTask(plugin.plugin(), () -> client.kick(event.cancelReason()));
                return;
            }

            plugin.err("Your LockLogin instance is not using any auth process. This is a security risk!");
            client.session().login(true);
            client.session().totpLogin(true);
            client.session().pinLogin(true);

            PluginPermissionManager<OfflinePlayer, String> manager = plugin.getPermissionManager();
            Player player = UserDataHandler.getPlayer(client);
            if (player != null && manager != null) {
                manager.restorePermissions(player);
                manager.applyGrants(player);
            }

            return;
        }

        if (process == null) {
            boolean log = true;
            if (previous != null) log = previous.wasSuccess();

            //Final auth step
            if (log) {
                EntityAuthenticateEvent event = new EntityAuthenticateEvent(client, true);
                plugin.moduleManager().fireEvent(event);

                if (event.isCancelled())
                    Bukkit.getScheduler().runTask(plugin.plugin(), () -> client.kick(event.cancelReason()));

                client.session().login(true);
                client.session().totpLogin(true);
                client.session().pinLogin(true);
            } else {
                EntityAuthenticateEvent event = new EntityAuthenticateEvent(client, false);
                event.setCancelled(true, "&cInvalid session status");
                plugin.moduleManager().fireEvent(event);

                if (event.isCancelled()) {
                    Bukkit.getScheduler().runTask(plugin.plugin(), () -> client.kick(event.cancelReason()));
                }
            }

            return;
        }

        if (previous != null && !previous.wasSuccess()) {
            return;
        }

        boolean allow = false;
        switch (process.getAuthType()) {
            case ANY:
                allow = true;
                break;
            case LOGIN:
                allow = client.account().isRegistered();
                break;
            case REGISTER:
                allow = client.account().id() <= 0 || !client.account().isRegistered();
                break;
        }

        if (allow) {
            Bukkit.getServer().getScheduler().runTask(plugin.plugin(), () -> {
                boolean work = true;
                if (process.name().equals(SpigotLoginProcess.getName()) ||
                        process.name().equals(SpigotPinProcess.getName()) ||
                            process.name().equals(SpigotRegisterProcess.getName())) {
                    work = process.isEnabled();
                }

                if (work) {
                    passedProcess.add(client.uniqueId());

                    FutureTask<AuthProcess> future = process.process(previous);
                    future.whenComplete(((authProcess, error) -> {
                        if (error != null) {
                            client.kick("&cAn error occurred while processing your session");
                            return;
                        }

                        startAuthProcess(client, authProcess); //Go to the next auth process

                        EntityProcessEvent event = new EntityProcessEvent(client, authProcess.wasSuccess(), authProcess.authProcess());
                        plugin.moduleManager().fireEvent(event);
                    }));
                } else {
                    startAuthProcess(client, previous);
                }
            });
        } else {
            startAuthProcess(client, previous); //Go directly to the next process
        }
    }

    private void validateUser(final AsyncPlayerPreLoginEvent e,
                              final LocalNetworkClient client,
                              final UUID offline_uid,
                              final UUID online_uid) {
        String name = e.getName();
        UUID provided_id = e.getUniqueId();

        UserSession session = client.session();
        Messages messages = plugin.messages();

        EventHelper.generateCaptcha(session);

        AtomicReference<UUID> use_uid = new AtomicReference<>(offline_uid);
        if (online_uid != null) {
            PremiumConfiguration premiumConfig = configuration.premium();
            if (premiumConfig.enable() || plugin.onlineMode()) {
                if (plugin.onlineMode()) {
                    use_uid.set(online_uid);
                } else {
                    if ((configuration.premium().auto() || client.connection().equals(ConnectionType.ONLINE)) && !premiumConfig.forceOfflineId()) {
                        use_uid.set(online_uid);
                    }
                }
            }
        }

        InetAddress address = e.getAddress();
        boolean result = invalidIP(address);

        EntityValidationEvent ip_validation_event = new EntityValidationEvent(client);
        ip_validation_event.setCancelled(result, (result ? "IP address is not valid" : null));
        plugin.moduleManager().fireEvent(ip_validation_event);

        if (ip_validation_event.isCancelled()) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Colorize.colorize(messages.ipProxyError() + "\n\n" + ip_validation_event.cancelReason()));
            plugin.logInfo("Denied access of player {0} for {1}", name, ip_validation_event.cancelReason());

            return;
        }

        if (plugin.bungeeMode()) {
            session.invalidate();
        } else {
            session.validate();

            if (EventHelper.checkMultiAccounts(e, name, address, client, messages)) return;
            if (EventHelper.checkBan(e, address, messages)) return;
            if (EventHelper.checkUUID(e, use_uid, messages)) return;
            if (EventHelper.checkName(e, messages)) return;
            if (EventHelper.checkOnline(e, use_uid, address, messages)) return;

            EntityPreConnectEvent event = new EntityPreConnectEvent(client);
            plugin.moduleManager().fireEvent(event);

            if (event.isCancelled()) {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Colorize.colorize(event.cancelReason()));
            }
        }
    }
}