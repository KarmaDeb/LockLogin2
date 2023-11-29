package es.karmadev.locklogin.spigot.util;

import es.karmadev.api.minecraft.uuid.UUIDFetcher;
import es.karmadev.api.minecraft.uuid.UUIDType;
import es.karmadev.api.spigot.core.KarmaPlugin;
import es.karmadev.api.spigot.inventory.helper.option.OptionsInventory;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.api.user.session.check.SessionChecker;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import es.karmadev.locklogin.spigot.util.storage.PlayerLocationStorage;
import es.karmadev.locklogin.spigot.util.storage.SpawnLocationStorage;
import es.karmadev.locklogin.spigot.util.window.settings.SettingsButton;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin user data handler
 */
public class UserDataHandler {

    private final static Set<UUID> readyToHandle = ConcurrentHashMap.newKeySet();
    private final static Map<UUID, Runnable> teleporting = new ConcurrentHashMap<>();
    private final static Map<UUID, OptionsInventory<SettingsButton>> settings = new ConcurrentHashMap<>();


    public static boolean isReady(final Player player) {
        return readyToHandle.contains(player.getUniqueId());
    }

    public static void setReady(final Player player) {
        readyToHandle.add(player.getUniqueId());
    }

    public static void handleDisconnect(final Player player) {
        readyToHandle.remove(player.getUniqueId());
    }

    public static boolean isTeleporting(final Player player) {
        return teleporting.containsKey(player.getUniqueId());
    }

    public static void setTeleporting(final Player player, final Runnable signal) {
        if (!teleporting.containsKey(player.getUniqueId()) && signal != null) {
            teleporting.put(player.getUniqueId(), signal);
            return;
        }

        teleporting.remove(player.getUniqueId());
    }

    public static void callTeleportSignal(final Player player) {
        if (teleporting.containsKey(player.getUniqueId())) {
            Runnable signal = teleporting.get(player.getUniqueId());
            if (signal == null) return;

            signal.run();
        }
    }

    public static NetworkClient fromEvent(final PlayerEvent event) {
        Player player = event.getPlayer();
        LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();

        int networkId = getNetworkId(player);
        if (networkId <= 0) {
            if (UserDataHandler.isReady(player)) {
                if (event instanceof Cancellable) {
                    Cancellable cancellable = (Cancellable) event;
                    cancellable.setCancelled(true);
                }
            }

            return null;
        }

        NetworkClient client = spigot.network().getPlayer(networkId);
        if (client == null) {
            if (event instanceof Cancellable) {
                Cancellable cancellable = (Cancellable) event;
                cancellable.setCancelled(true);
            }
        }

        return client;
    }

    public static int getNetworkId(final Player player) {
        LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();
        KarmaPlugin plugin = spigot.plugin();

        if (player.hasMetadata("networkId")) {
            List<MetadataValue> metas = player.getMetadata("networkId");
            for (MetadataValue meta : metas) {
                Plugin owner = meta.getOwningPlugin();
                if (owner != null && owner.isEnabled()) {
                    if (owner.equals(plugin)) {
                        return meta.asInt();
                    }
                }
            }
        }
        
        return -1;
    }

    public static Player getPlayer(final NetworkClient client) {
        int id = client.id();
        for (Player online : Bukkit.getOnlinePlayers()) {
            int networkId = getNetworkId(online);
            if (networkId == id) return online;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static OptionsInventory<SettingsButton> getSettings(final NetworkClient client) {
        return settings.computeIfAbsent(client.uniqueId(), (i) -> {
            UserAccount account = client.account();
            UserSession session = client.session();
            PremiumDataStore premium = CurrentPlugin.getPlugin().premiumStore();

            LockLogin plugin = CurrentPlugin.getPlugin();
            assert plugin != null;

            LockLoginSpigot spigot = (LockLoginSpigot) plugin;

            OptionsInventory<SettingsButton> settings = new OptionsInventory<>("Account", 9 * 6);
            settings.setCanClose(true);

            settings.addChoice(SettingsButton.LOGOUT, 4, SettingsButton.LOGOUT.toItemStack()).onClick(
                    (inventory, event, player) -> {
                        session.login(false);
                        session.pinLogin(false);
                        session.totpLogin(false);

                        SessionChecker checker = client.getSessionChecker();
                        checker.restart();

                        Location spawn = SpawnLocationStorage.load();
                        Location current = player.getLocation();
                        boolean saveLocation = true;
                        if (spawn != null) {
                            double distance = spawn.distance(current);
                            if (distance <= plugin.configuration().spawn().spawnRadius()) {
                                saveLocation = false;
                            }
                        }

                        if (saveLocation) {
                            PlayerLocationStorage storage = new PlayerLocationStorage(client);
                            storage.assign(player.getLocation());
                        }

                        if (spawn != null) {
                            player.teleport(spawn);
                        }

                        spigot.plugin().getJoinHandler().startAuthProcess(client, null);
                        inventory.close(player);
                    }
            );
            settings.addChoice(SettingsButton.TOGGLE_SESSION, 19, SettingsButton.TOGGLE_SESSION.toItemStack()).onClick(
                    (inventory, event, player) -> {
                        if (plugin.configuration().session().enable()) {
                            session.persistent(!session.isPersistent());
                            if (session.isPersistent()) {
                                client.sendMessage(plugin.messages().prefix() + plugin.messages().sessionEnabled());
                            } else {
                                client.sendMessage(plugin.messages().prefix() + plugin.messages().sessionDisabled());
                            }
                        } else {
                            client.sendMessage(plugin.messages().prefix() + plugin.messages().sessionServerDisabled());
                        }

                        inventory.close(player);
                    }
            );
            settings.addChoice(SettingsButton.TOGGLE_PREMIUM, 25, SettingsButton.TOGGLE_PREMIUM.toItemStack()).onClick(
                    (inventory, event, player) -> Bukkit.getServer().getScheduler().runTaskAsynchronously(spigot.plugin(), () -> {
                        if (plugin.configuration().premium().enable()) {
                            if (client.connection().equals(ConnectionType.ONLINE)) {
                                client.setConnection(ConnectionType.OFFLINE);
                                client.kick(plugin.messages().premiumDisabled());
                            } else {
                                if (!premium.exists(account.name())) {
                                    UUID online = UUIDFetcher.fetchUUID(account.name(), UUIDType.ONLINE);
                                    if (online == null) {
                                        client.sendMessage(plugin.messages().prefix() + plugin.messages().premiumFailAuth());

                                        Bukkit.getServer().getScheduler().runTask(spigot.plugin(), () -> inventory.close(player));
                                        return;
                                    }

                                    premium.saveId(account.name(), online);
                                }

                                client.setConnection(ConnectionType.ONLINE);
                                client.kick(plugin.messages().premiumEnabled());
                            }

                            return;
                        } else {
                            client.sendMessage(plugin.messages().prefix() + plugin.messages().premiumError());
                        }

                        Bukkit.getServer().getScheduler().runTask(spigot.plugin(), () -> inventory.close(player));
                    })
            );

            return settings;
        });
    }

    public static void destroySettings(final LocalNetworkClient client) {
        settings.remove(client.uniqueId());
    }
}
