package es.karmadev.locklogin.redis.api;

import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.redis.network.RedisChannel;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the redis service
 */
public interface RedisService extends PluginService {

    /**
     * Get if the service is connected
     * to the redis server
     *
     * @return if the service is connected
     */
    boolean isConnected();

    /**
     * Create a new channel on the redis
     * network
     *
     * @param name the channel name
     * @return the redis channel
     */
    CompletableFuture<RedisChannel> createChannel(final String name);
}
