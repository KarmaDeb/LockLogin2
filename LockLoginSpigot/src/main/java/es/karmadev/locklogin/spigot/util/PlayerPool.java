package es.karmadev.locklogin.spigot.util;

import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.common.util.ActionListener;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import ml.karmaconfigs.api.bukkit.KarmaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Player pool
 */
public class PlayerPool {

    private final Map<UUID, String> ids = new ConcurrentHashMap<>();
    private final ActionListener<Player> actionExecutor;
    private final ScheduledThreadPoolExecutor executor;
    private boolean running = false;

    public PlayerPool(final ActionListener<Player> action) {
        actionExecutor = action;
        executor = new ScheduledThreadPoolExecutor(1);
    }

    public void schedule() {
        if (running) return;
        running = true;

        LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();
        KarmaPlugin plugin = spigot.plugin();
        executor.scheduleAtFixedRate(() -> {
            Set<UUID> remove = new HashSet<>();
            for (UUID id : ids.keySet()) {
                String reason = ids.get(id);

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null && player.isOnline()) {
                        actionExecutor.onAction(reason, player);
                        remove.add(id);
                    }
                });
            }

            remove.forEach(ids::remove);
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void add(final UUID id, final String reason) {
        ids.put(id, reason);
    }

    public void remove(final UUID id) {
        ids.remove(id);
    }
}
