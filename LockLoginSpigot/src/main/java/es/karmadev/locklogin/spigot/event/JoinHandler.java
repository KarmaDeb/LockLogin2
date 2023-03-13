package es.karmadev.locklogin.spigot.event;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.Match;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.event.entity.client.EntityCreatedEvent;
import es.karmadev.locklogin.api.event.entity.client.EntityValidationEvent;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.plugin.file.Messages;
import es.karmadev.locklogin.api.plugin.file.section.PremiumConfiguration;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.common.api.client.COnlineClient;
import es.karmadev.locklogin.common.api.client.CPremiumDataStore;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import ml.karmaconfigs.api.common.string.StringUtils;
import ml.karmaconfigs.api.common.timer.SchedulerUnit;
import ml.karmaconfigs.api.common.timer.SourceScheduler;
import ml.karmaconfigs.api.common.timer.scheduler.SimpleScheduler;
import ml.karmaconfigs.api.common.utils.uuid.OKAResponse;
import ml.karmaconfigs.api.common.utils.uuid.UUIDType;
import ml.karmaconfigs.api.common.utils.uuid.UUIDUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinHandler implements Listener {

    private final LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final Map<UUID, UUID> uuid_translator = new ConcurrentHashMap<>();

    private static final String IPV4_REGEX =
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
    private static final Pattern IPv4_PATTERN = Pattern.compile(IPV4_REGEX);

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        Messages messages = plugin.messages();
        if (!plugin.runtime().isBooted()) {
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
            online_uid = response.getId(UUIDType.ONLINE);

            premium.saveId(name, online_uid);
        }

        LocalNetworkClient offline = plugin.network().getOfflinePlayer(offline_uid);
        if (offline == null) {
            offline = plugin.getUserFactory(false).create(name, offline_uid);
            EntityCreatedEvent event = new EntityCreatedEvent(offline);
            plugin.moduleManager().fireEvent(event);
        }

        if (online_uid != null && !online_uid.equals(offline_uid)) uuid_translator.put(online_uid, offline_uid);
        UUID use_uid = offline_uid;
        if (online_uid != null) {
            PremiumConfiguration premiumConfig = plugin.configuration().premium();
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

            //TODO: Check potential alt accounts
            //TODO: Create BruteForce handler

            if (plugin.configuration().verifyUniqueIDs()) {
                if (!provided_id.equals(use_uid)) {
                    boolean allow = false;

                    //TODO: Add compatibility with FloodGate
                }
            }

            //TODO: Check names
            Player online = plugin.getServer().getPlayer(use_uid);
            if (online != null && plugin.configuration().allowSameIp()) {
                InetSocketAddress online_address = online.getAddress();
                if (online_address != null && online_address.getAddress() != null && address != null) {
                    InetAddress online_inet = online_address.getAddress();
                    if (!Arrays.equals(online_inet.getAddress(), address.getAddress())) {
                        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, StringUtils.toColor(messages.alreadyPlaying()));
                        return;
                    }
                }
            }

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

            LocalNetworkClient offline = plugin.network().getOfflinePlayer(id);
            COnlineClient online = new COnlineClient(offline.id(), plugin.sqlite, null);

            plugin.network.appendClient(online);

        });
    }

    private boolean invalidIP(final InetAddress address) {
        if (plugin.configuration().verifyIpAddress()) {
            if (StringUtils.isNullOrEmpty(address)) {
                return true;
            }

            Matcher matcher = IPv4_PATTERN.matcher(address.getHostAddress());
            return !matcher.matches();
        }

        return false;
    }
}
