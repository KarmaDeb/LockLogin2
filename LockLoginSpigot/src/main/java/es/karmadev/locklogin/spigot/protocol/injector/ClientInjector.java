package es.karmadev.locklogin.spigot.protocol.injector;

import es.karmadev.api.schedule.runner.async.AsyncTaskExecutor;
import es.karmadev.api.spigot.core.KarmaPlugin;
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

    /**
     * Inject the player
     *
     * @param player the client to inject
     * @return the client injection
     */
    public Injection inject(final Player player) {
        Injection result = injections.computeIfAbsent(player.getUniqueId(), (injection) -> new Injection());
        result.inject(player);

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