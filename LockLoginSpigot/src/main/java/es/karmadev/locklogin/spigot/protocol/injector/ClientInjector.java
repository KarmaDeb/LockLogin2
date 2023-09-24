package es.karmadev.locklogin.spigot.protocol.injector;

import es.karmadev.api.spigot.core.KarmaPlugin;
import es.karmadev.api.schedule.runner.async.AsyncTaskExecutor;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.client.NetworkClient;
import es.karmadev.locklogin.common.util.ActionListener;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * LockLogin client injector
 */
public class ClientInjector {

    private final ConcurrentMap<UUID, Injection> injections = new ConcurrentHashMap<>();
    private ConnectionPool pool;

    public ClientInjector() {
        ActionListener<Player> listener = (unused, player) -> {
            Injection injection = inject(player);
            if (injection.isInjected()) {
                pool.remove(player.getUniqueId());
            }
        };
        pool = new ConnectionPool(listener);
        pool.schedule();
    }

    /**
     * Inject the player
     *
     * @param player the client to inject
     * @return the client injection
     */
    public Injection inject(final Player player) {
        Injection result = injections.computeIfAbsent(player.getUniqueId(), (injection) -> new Injection());
        Object playerConnection = Injection.playerConnection(Injection.toEntityHandle(player));
        if (playerConnection != null) {
            result.inject(player);
        } else {
            pool.add(player.getUniqueId(), player.getName());
        }

        return result;
    }

    /**
     * Release the player
     *
     * @param client the client to release
     */
    public void release(final NetworkClient client) {
        Injection result = injections.remove(client.uniqueId());
        if (result == null) return;

        result.release();
    }
}

/**
 * Player pool
 */
class ConnectionPool {

    private final Map<UUID, String> ids = new ConcurrentHashMap<>();
    private final ActionListener<Player> actionExecutor;
    private boolean running = false;

    public ConnectionPool(final ActionListener<Player> action) {
        actionExecutor = action;
    }

    public void schedule() {
        if (running) return;
        running = true;

        LockLoginSpigot spigot = (LockLoginSpigot) CurrentPlugin.getPlugin();
        KarmaPlugin plugin = spigot.plugin();
        AsyncTaskExecutor.EXECUTOR.scheduleAtFixedRate(() -> {
            for (UUID id : ids.keySet()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        Object handle = Injection.toEntityHandle(player);
                        Object connection = Injection.playerConnection(handle);

                        if (connection != null) {
                            actionExecutor.onAction(player.getName(), player);
                        }
                    }
                });
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    public void add(final UUID id, final String reason) {
        ids.put(id, reason);
    }

    public void remove(final UUID id) {
        ids.remove(id);
    }
}