package es.karmadev.locklogin.spigot.util;

import es.karmadev.api.spigot.core.KarmaPlugin;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LockLogin user data handler
 */
public class UserDataHandler {

    private final static Set<UUID> readyToHandle = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static boolean isReady(final Player player) {
        return readyToHandle.contains(player.getUniqueId());
    }

    public static void setReady(final Player player) {
        readyToHandle.add(player.getUniqueId());
    }

    public static void handleDisconnect(final Player player) {
        readyToHandle.remove(player.getUniqueId());
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
}
