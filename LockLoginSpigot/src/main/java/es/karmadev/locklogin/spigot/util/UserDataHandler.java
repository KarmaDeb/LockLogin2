package es.karmadev.locklogin.spigot.util;

import es.karmadev.api.minecraft.uuid.UUIDFetcher;
import es.karmadev.api.minecraft.uuid.UUIDType;
import es.karmadev.api.spigot.core.KarmaPlugin;
import es.karmadev.api.spigot.inventory.helper.option.OptionsInventory;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.ConnectionType;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.network.client.offline.LocalNetworkClient;
import es.karmadev.locklogin.api.user.account.UserAccount;
import es.karmadev.locklogin.api.user.premium.PremiumDataStore;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
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
            OptionsInventory<SettingsButton> settings = new OptionsInventory<>("Account", 9 * 6);
            settings.setCanClose(true);

            LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();

            settings.addChoice(SettingsButton.TOGGLE_SESSION, 19, SettingsButton.TOGGLE_SESSION.toItemStack()).onClick(
                    (inventory, event, player) -> {
                        NetworkClient user = spigot.network().getPlayer(player.getUniqueId());
                        UserSession session = user.session();

                        if (spigot.configuration().session().enable()) {
                            session.persistent(!session.isPersistent());
                            if (session.isPersistent()) {
                                user.sendMessage(spigot.messages().prefix() + spigot.messages().sessionEnabled());
                            } else {
                                user.sendMessage(spigot.messages().prefix() + spigot.messages().sessionDisabled());
                            }
                        } else {
                            user.sendMessage(spigot.messages().prefix() + spigot.messages().sessionServerDisabled());
                        }

                        inventory.close(player);
                    }
            );
            settings.addChoice(SettingsButton.TOGGLE_PREMIUM, 25, SettingsButton.TOGGLE_PREMIUM.toItemStack()).onClick(
                    (inventory, event, player) -> {
                        Bukkit.getServer().getScheduler().runTaskAsynchronously(spigot.plugin(), () -> {
                            NetworkClient user = spigot.network().getPlayer(player.getUniqueId());
                            PremiumDataStore premium = spigot.premiumStore();
                            UserAccount account = user.account();

                            if (spigot.configuration().premium().enable()) {
                                if (user.connection().equals(ConnectionType.ONLINE)) {
                                    user.setConnection(ConnectionType.OFFLINE);
                                    user.kick(spigot.messages().premiumDisabled());
                                } else {
                                    if (!premium.exists(account.name())) {
                                        UUID online = UUIDFetcher.fetchUUID(account.name(), UUIDType.ONLINE);
                                        if (online == null) {
                                            user.sendMessage(spigot.messages().prefix() + spigot.messages().premiumFailAuth());

                                            Bukkit.getServer().getScheduler().runTask(spigot.plugin(), () -> inventory.close(player));
                                            return;
                                        }

                                        premium.saveId(account.name(), online);
                                    }

                                    user.setConnection(ConnectionType.ONLINE);
                                    user.kick(spigot.messages().premiumEnabled());
                                }

                                return;
                            } else {
                                user.sendMessage(spigot.messages().prefix() + spigot.messages().premiumError());
                            }

                            Bukkit.getServer().getScheduler().runTask(spigot.plugin(), () -> inventory.close(player));
                        });
                    }
            );

            return settings;
        });
    }

    public static void destroySettings(final LocalNetworkClient client) {
        settings.remove(client.uniqueId());
    }
}
