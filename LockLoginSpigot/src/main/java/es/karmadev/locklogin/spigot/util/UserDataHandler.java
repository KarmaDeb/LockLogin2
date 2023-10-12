package es.karmadev.locklogin.spigot.util;

import es.karmadev.api.spigot.core.KarmaPlugin;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
