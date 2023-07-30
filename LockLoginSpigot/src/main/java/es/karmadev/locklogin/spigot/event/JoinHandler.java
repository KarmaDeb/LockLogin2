package es.karmadev.locklogin.spigot.event;

import es.karmadev.api.core.scheduler.SpigotTask;
import es.karmadev.api.logger.log.console.ConsoleColor;
import es.karmadev.api.minecraft.color.ColorComponent;
import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.spigot.reflection.actionbar.SpigotActionbar;
import es.karmadev.api.spigot.reflection.title.SpigotTitle;
import es.karmadev.api.strings.ListSpacer;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.api.web.minecraft.MineAPI;
import es.karmadev.api.web.minecraft.UUIDType;
import es.karmadev.api.web.minecraft.response.data.OKARequest;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.event.entity.client.EntityCreatedEvent;
import es.karmadev.locklogin.api.event.entity.client.EntitySessionCreatedEvent;
import es.karmadev.locklogin.api.event.entity.client.EntityValidationEvent;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.file.section.CaptchaConfiguration;
import es.karmadev.locklogin.api.plugin.file.section.PremiumConfiguration;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.plugin.service.floodgate.FloodGateService;
import es.karmadev.locklogin.api.plugin.service.name.NameValidator;
import es.karmadev.locklogin.api.security.brute.BruteForceService;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.SessionFactory;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.api.user.session.check.SessionChecker;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.client.CLocalClient;
import es.karmadev.locklogin.common.api.client.COnlineClient;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.protocol.ProtocolAssistant;
import es.karmadev.locklogin.spigot.util.PlayerPool;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class JoinHandler implements Listener {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final Configuration configuration = plugin.configuration();
    private final Messages messages = plugin.messages();

    private final Map<UUID, UUID> uuid_translator = new ConcurrentHashMap<>();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    private final PlayerPool playerKickPool = new PlayerPool((reason, player) -> player.kickPlayer(ConsoleColor.parse(reason)));

    public JoinHandler() {
        playerKickPool.schedule();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (plugin.getRuntime().booting()) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ConsoleColor.parse("&cThe server is booting!"));
            return;
        }

        plugin.plugin().scheduler("async").schedule(() -> {
            String name = e.getName();
            PremiumDataStore premium = CurrentPlugin.getPlugin().premiumStore();

            UUID provided_id = e.getUniqueId();
            UUID offline_uid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
            UUID online_uid = premium.onlineId(name);
            if (online_uid == null) {
                OKARequest request = MineAPI.fetchAndWait(name);
                online_uid = request.getUUID(UUIDType.ONLINE);

                if (online_uid != null) {
                    premium.saveId(name, online_uid);
                }
            }

            CLocalClient offline = (CLocalClient) plugin.network().getOfflinePlayer(offline_uid);
            if (offline == null) {
                offline = (CLocalClient) plugin.getUserFactory(false).create(name, offline_uid);
                EntityCreatedEvent event = new EntityCreatedEvent(offline);
                plugin.moduleManager().fireEvent(event);
            }

            UserSession session = offline.session();
            if (session == null) {
                SessionFactory<? extends UserSession> factory = plugin.getSessionFactory(false);
                session = factory.create(offline);

                EntitySessionCreatedEvent event = new EntitySessionCreatedEvent(offline, session);
                plugin.moduleManager().fireEvent(event);
            }

            if (!session.isCaptchaLogged()) {
                CaptchaConfiguration settings = configuration.captcha();
                if (settings.enable()) {
                    int size = settings.length();
                    String captcha = StringUtils.generateString(size);

                    //String captcha = string.create();
                    if (settings.strikethrough()) {
                        if (settings.randomStrike()) {
                            String last_color = ConsoleColor.lastColor(captcha);
                            StringBuilder builder = new StringBuilder();

                            for (int i = 0; i < captcha.length(); i++) {
                                int random = new Random().nextInt(100);

                                if (random > 50) {
                                    builder.append(last_color).append("&m").append(captcha.charAt(i)).append("&r");
                                } else {
                                    builder.append(last_color).append(captcha.charAt(i)).append("&r");
                                }
                            }

                            captcha = builder.toString();
                        } else {
                            captcha = "&m" + captcha;
                        }
                    }

                    session.setCaptcha(captcha);
                } else {
                    session.captchaLogin(true);
                }
            }

            UUID use_uid = offline_uid;
            if (online_uid != null) {
                PremiumConfiguration premiumConfig = configuration.premium();
                if (premiumConfig.enable() || plugin.onlineMode()) {
                    if (plugin.onlineMode()) {
                        use_uid = online_uid;
                    } else {
                        if (offline.connection().equals(ConnectionType.ONLINE) && !premiumConfig.forceOfflineId()) {
                            use_uid = online_uid;
                        }
                    }
                }
            }

            InetAddress address = e.getAddress();
            boolean result = invalidIP(address);

            EntityValidationEvent ip_validation_event = new EntityValidationEvent(offline);
            ip_validation_event.setCancelled(result, (result ? "IP address is not valid" : null));
            plugin.moduleManager().fireEvent(ip_validation_event);

            if (ip_validation_event.isCancelled()) {
                playerKickPool.add(use_uid, messages.ipProxyError() + "\n\n" + ip_validation_event.cancelReason());
                plugin.logInfo("Denied access of player {0} for {1}", name, ip_validation_event.cancelReason());

                return;
            }

            if (plugin.bungeeMode()) {
                session.invalidate();
            } else {
                session.validate();

                MultiAccountManager multi = plugin.accountManager();
                if (multi == null || multi.allow(address, configuration.register().maxAccounts())) {
                    if (multi != null) {
                        multi.assign(offline, address);

                        int amount = multi.getAccounts(address).size();
                        int max = configuration.register().maxAccounts();
                        if (amount >= max && max > 1) { //We only want to send a warning if the maximum amount of accounts is over 1
                            for (NetworkClient client : plugin.network().getOnlinePlayers()) {
                                if (client.hasPermission(LockLoginPermission.PERMISSION_INFO_ALT_ALERT)) {
                                    client.sendMessage(messages.prefix() + messages.altFound(name, amount));
                                }
                            }
                        }
                    }
                } else {
                    playerKickPool.add(use_uid, messages.maxRegisters());
                    return;
                }

                PluginService bf_service = plugin.getService("bruteforce");
                if (bf_service instanceof BruteForceService) {
                    BruteForceService bruteforce = (BruteForceService) bf_service;

                    if (bruteforce.isBlocked(address)) {
                        long timeLeft = bruteforce.banTimeLeft(address);

                        //BFAP = Brute Force Attack Protector
                        plugin.logWarn("[BFAP] Address {0} tried to access the server but was blocked for brute force attack. Ban time ramining: {1}", address.getHostAddress(), TimeUnit.MILLISECONDS.toSeconds(timeLeft));

                        playerKickPool.add(use_uid, messages.ipBlocked(timeLeft));
                        return;
                    }
                }

                if (configuration.verifyUniqueIDs()) {
                    if (!provided_id.equals(use_uid)) {
                        boolean deny = true;

                        PluginService fg_service = plugin.getService("floodgate");
                        if (fg_service instanceof FloodGateService) {
                            FloodGateService floodgate = (FloodGateService) fg_service;
                            if (floodgate.isBedrock(provided_id)) {
                                deny = false;
                                use_uid = provided_id;
                            }
                        }

                        if (deny) {
                            //USP = UUID Spoofer Protector
                            plugin.logWarn("[USP] Denied connection from {0} because its UUID ({1}) doesn't match with generated one ({2})", name, use_uid, provided_id);

                            playerKickPool.add(use_uid, messages.uuidFetchError());
                            return;
                        }
                    }
                }

                PluginService name_service = plugin.getService("name");
                if (name_service instanceof ServiceProvider) {
                    ServiceProvider<? extends PluginService> provider = (ServiceProvider<?>) name_service;
                    PluginService service = provider.serve();

                    if (service instanceof NameValidator) {
                        NameValidator validator = (NameValidator) service;
                        validator.validate(name);

                        if (validator.isValid()) {
                            plugin.logInfo("Successfully validated username of {0}", name);
                        } else {
                            boolean deny = true;

                            PluginService fg_service = plugin.getService("floodgate");
                            if (fg_service instanceof FloodGateService) {
                                FloodGateService floodgate = (FloodGateService) fg_service;
                                if (floodgate.isBedrock(provided_id)) {
                                    deny = false;
                                    plugin.info("Connected bedrock client {0}", name);
                                }
                            }

                            if (deny) {
                                //NVP = Name Validator Protector
                                plugin.logWarn("[NVP] Denied connection from {0} because its name was not valid ({1})", name, ConsoleColor.strip(validator.invalidCharacters()));
                                playerKickPool.add(use_uid, messages.illegalName(validator.invalidCharacters()));
                                return;
                            }
                        }
                    }
                }

                Player online = Bukkit.getServer().getPlayer(offline_uid);
                if (online == null && online_uid != null) online = Bukkit.getServer().getPlayer(online_uid);
                if (online != null && online.isOnline() && configuration.allowSameIp()) {
                    InetSocketAddress online_address = online.getAddress();
                    if (online_address != null && online_address.getAddress() != null) {
                        InetAddress online_inet = online_address.getAddress();
                        if (!Arrays.equals(online_inet.getAddress(), address.getAddress())) {
                            playerKickPool.add(use_uid, messages.alreadyPlaying());
                            return;
                        }
                    }
                }

                if (online_uid != null && !online_uid.equals(offline_uid)) uuid_translator.put(online_uid, offline_uid);
            }
        });
        e.allow();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPostJoin(final PlayerJoinEvent e) {
        Player player = e.getPlayer();

        String message = e.getJoinMessage();
        e.setJoinMessage("");

        plugin.plugin().scheduler("async").schedule(() -> {
            UUID id = player.getUniqueId();
            if (uuid_translator.containsKey(id)) {
                id = uuid_translator.getOrDefault(id, null);
                if (id == null) id = player.getUniqueId();
            }

            CLocalClient offline = (CLocalClient) plugin.network().getOfflinePlayer(id);
            COnlineClient online = new COnlineClient(offline.id(), plugin.driver(), null)
                    .onMessageRequest((msg) -> {
                        if (!player.isOnline()) return;
                        player.sendMessage(ColorComponent.parse(msg)
                                .replace("{player}", player.getName())
                                .replace("{server}", configuration.server())
                                .replace("{ServerName}", configuration.server()));
                    })
                    .onActionBarRequest((msg) -> {
                        if (!player.isOnline()) return;

                        SpigotActionbar bar = new SpigotActionbar(msg.replace("{player}", player.getName())
                                .replace("{server}", configuration.server())
                                .replace("{ServerName}", configuration.server()));
                        bar.send(player);
                    })
                    .onTitleRequest((msg) -> {
                        if (!player.isOnline()) return;

                        SpigotTitle title = new SpigotTitle(msg.title().replace("{player}", player.getName())
                                .replace("{server}", configuration.server())
                                .replace("{ServerName}", configuration.server()), msg.subtitle().replace("{player}", player.getName())
                                .replace("{server}", configuration.server())
                                .replace("{ServerName}", configuration.server()));
                        title.send(player, msg.fadeIn(), msg.show(), msg.fadeOut());
                    })
                    .onKickRequest((msg) -> {
                        if (!player.isOnline()) return;

                        List<String> reasons = Arrays.asList(msg);
                        String reason = StringUtils.listToString(ColorComponent.parse(reasons, ArrayList::new), ListSpacer.NEW_LINE).replace("{player}", player.getName())
                                .replace("{server}", configuration.server())
                                .replace("{ServerName}", configuration.server());

                        SpigotTask task = plugin.plugin().scheduler("sync").schedule(() -> player.kickPlayer(reason));
                        task.markSynchronous();
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

            player.setMetadata("networkId", new FixedMetadataValue(plugin.plugin(), online.id()));
            SessionChecker checker = online.getSessionChecker();
            plugin.plugin().scheduler("async").schedule(checker);

            if (online.hasPermission(LockLoginPermission.PERMISSION_JOIN_SILENT)) return;
            if (configuration.clearChat()) {
                ProtocolAssistant.clearChat(player);
            }

            String customMessage = messages.join(online);
            if (!ObjectUtils.isNullOrEmpty(customMessage)) {
                Bukkit.broadcastMessage(ColorComponent.parse(message));
            }
        });
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
}
