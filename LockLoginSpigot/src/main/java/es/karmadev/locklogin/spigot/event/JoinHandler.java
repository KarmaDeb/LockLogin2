package es.karmadev.locklogin.spigot.event;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.event.entity.client.EntityCreatedEvent;
import es.karmadev.locklogin.api.event.entity.client.EntityValidationEvent;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.data.MultiAccountManager;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Configuration;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.file.section.PremiumConfiguration;
import es.karmadev.locklogin.api.plugin.permission.LockLoginPermission;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.api.plugin.service.floodgate.FloodGateService;
import es.karmadev.locklogin.api.plugin.service.name.NameValidator;
import es.karmadev.locklogin.api.security.brute.BruteForceService;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.CPluginNetwork;
import es.karmadev.locklogin.common.api.client.CLocalClient;
import es.karmadev.locklogin.common.api.client.COnlineClient;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.utils.uuid.OKAResponse;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
import ml.karmaconfigs.api.common.utils.uuid.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        if (plugin.runtime().booting()) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor("&cThe server is booting!"));
            return;
        }

        String name = e.getName();
        PremiumDataStore premium = CurrentPlugin.getPlugin().premiumStore();

        UUID provided_id = e.getUniqueId();
        UUID offline_uid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        UUID online_uid = premium.onlineId(name);
        if (online_uid == null) {
            OKAResponse response = UUIDUtil.fetchOKA(name);
            if (response != null) {
                online_uid = response.getId(UUIDType.ONLINE);
                premium.saveId(name, online_uid);
            }
        }

        LocalNetworkClient offline = plugin.network().getOfflinePlayer(offline_uid);
        if (offline == null) {
            offline = plugin.getUserFactory(false).create(name, offline_uid);
            EntityCreatedEvent event = new EntityCreatedEvent(offline);
            plugin.moduleManager().fireEvent(event);
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
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, messages.ipProxyError() + "\n\n" + ip_validation_event.cancelReason());
            plugin.logInfo("Denied access of player {0} for {1}", name, ip_validation_event.cancelReason());

            return;
        }

        UserSession session = offline.session();
        if (plugin.bungeeMode()) {
            session.invalidate();
        } else {
            session.validate();

            MultiAccountManager multi = plugin.accountManager();
            if (multi.allow(address, configuration.register().maxAccounts())) {
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
            } else {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.maxRegisters()));
                return;
            }

            PluginService bf_service = plugin.getService("bruteforce");
            if (bf_service instanceof BruteForceService) {
                BruteForceService bruteforce = (BruteForceService) bf_service;

                if (bruteforce.isBlocked(address)) {
                    long timeLeft = bruteforce.banTimeLeft(address);

                    //BFAP = Brute Force Attack Protector
                    plugin.logWarn("[BFAP] Address {0} tried to access the server but was blocked for brute force attack. Ban time ramining: {1}", address.getHostAddress(), TimeUnit.MILLISECONDS.toSeconds(timeLeft));

                    e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, StringUtils.toColor(messages.ipBlocked(timeLeft)));
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

                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.uuidFetchError()));
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
                            plugin.logWarn("[NVP] Denied connection from {0} because its name was not valid ({1})", name, StringUtils.stripColor(validator.invalidCharacters()));
                            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.illegalName(validator.invalidCharacters())));
                            return;
                        }
                    }
                }
            }

            Player online = Bukkit.getServer().getPlayer(use_uid);
            if (online != null && configuration.allowSameIp()) {
                InetSocketAddress online_address = online.getAddress();
                if (online_address != null && online_address.getAddress() != null) {
                    InetAddress online_inet = online_address.getAddress();
                    if (!Arrays.equals(online_inet.getAddress(), address.getAddress())) {
                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.alreadyPlaying()));
                        return;
                    }
                }
            }

            if (online_uid != null && !online_uid.equals(offline_uid)) uuid_translator.put(online_uid, use_uid);
            e.allow();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPostJoin(final PlayerJoinEvent e) {
        Player player = e.getPlayer();

        CompletableFuture.runAsync(() -> {
            UUID id = player.getUniqueId();
            if (uuid_translator.containsKey(id)) {
                id = uuid_translator.getOrDefault(id, null);
                if (id == null) id = player.getUniqueId();
            }

            CLocalClient offline = (CLocalClient) plugin.network().getOfflinePlayer(id);
            COnlineClient online = new COnlineClient(offline.id(), plugin.driver(), null);

            CPluginNetwork network = (CPluginNetwork) plugin.network();
            network.appendClient(online);
        });
    }

    private boolean invalidIP(final InetAddress address) {
        if (configuration.verifyIpAddress()) {
            if (StringUtils.isNullOrEmpty(address)) {
                return true;
            }

            Matcher matcher = IPv4_PATTERN.matcher(address.getHostAddress());
            return !matcher.matches();
        }

        return false;
    }
}
