package es.karmadev.locklogin.spigot.util.process;

import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * The LoginProcessor is a processor
 * for ASyncPreLoginEvent, as some actions that
 * must run in LoginEvent require ASyncPreLoginEvent to be run
 * first
 */
public class ClientProcessor {

    private final ConcurrentMap<UUID, Boolean> clients = new ConcurrentHashMap<>();
    private final Set<UUID> preAllowedClients = ConcurrentHashMap.newKeySet();
    private final Consumer<Player> consumer;

    public ClientProcessor(final LockLoginSpigot spigot, final @NotNull Consumer<Player> consumer) {
        Plugin plugin = spigot.plugin();
        this.consumer = consumer;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Iterator<Map.Entry<UUID, Boolean>> iterator = clients.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Boolean> entry = iterator.next();
                if (entry.getValue()) {
                    iterator.remove();

                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        consumer.accept(player);
                    }
                }
            }
        }, 0, 1);
    }

    public void markForProcess(final UUID id) {
        if (clients.containsKey(id)) {
            clients.put(id, true);
        } else {
            preAllowedClients.add(id);
        }
    }

    public void appendProcessor(final UUID id) {
        if (preAllowedClients.contains(id)) {
            preAllowedClients.remove(id);
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                consumer.accept(player);
            }

            return;
        }

        clients.put(id, false);
    }
}
