package es.karmadev.locklogin.spigot.util;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.api.user.session.UserSession;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import org.bukkit.entity.Player;
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
    private final Player player;

    public UserDataHandler(final Player player) {
        this.player = player;
    }

    public boolean isReady() {
        return readyToHandle.contains(player.getUniqueId());
    }

    public void setReady() {
        readyToHandle.add(player.getUniqueId());
    }

    public void handleDisconnect() {
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
}
