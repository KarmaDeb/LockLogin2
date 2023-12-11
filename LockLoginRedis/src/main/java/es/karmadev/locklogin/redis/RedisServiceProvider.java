package es.karmadev.locklogin.redis;

import es.karmadev.locklogin.api.plugin.service.ServiceProvider;
import es.karmadev.locklogin.redis.api.RedisService;
import es.karmadev.locklogin.redis.api.options.RedisClusterOptions;

/**
 * Represents the redis service provider.
 * Redis is a high-end and fast key-value database,
 * useful for BungeeCord
 */
public final class RedisServiceProvider implements ServiceProvider<RedisService> {

    public RedisServiceProvider() {

    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "redis-provider";
    }

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return false;
    }

    /**
     * Serve a plugin service
     *
     * @param arguments the service arguments
     * @return the plugin service
     */
    @Override
    public RedisService serve(final Object... arguments) {
        if (arguments.length != 1) return null;
        Object parameter = arguments[0];
        if (parameter instanceof RedisClusterOptions) {
            RedisClusterOptions options = (RedisClusterOptions) parameter;
            return new DefaultRedisService(options);
        }

        return null;
    }

    /**
     * Get the service class
     *
     * @return the service class
     */
    @Override
    public Class<RedisService> getService() {
        return RedisService.class;
    }
}
