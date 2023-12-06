package es.karmadev.locklogin.redis;

import es.karmadev.api.object.var.Variable;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.redis.network.RedisChannel;
import es.karmadev.locklogin.redis.options.RedisClusterOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the redis service. This service must be
 * provided by a valid {@link es.karmadev.locklogin.api.plugin.service.ServiceProvider}, for example,
 * the default implementation ({@link RedisServiceProvider})
 */
public class RedisService implements PluginService {

    private final RedisClusterOptions options;
    private final Map<String, RedisChannel> channel = new ConcurrentHashMap<>();

    private final static Variable<JedisCluster> cluster = Variable.notNull(null);

    /**
     * Create the redis service
     */
    protected RedisService(final RedisClusterOptions options) {
        this.options = options;
    }

    /**
     * Get the service name
     *
     * @return the service name
     */
    @Override
    public String name() {
        return "redis-connector";
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
        return true;
    }

    /**
     * Create a new channel on the redis
     * network
     *
     * @param name the channel name
     * @return the redis channel
     */
    public RedisChannel createChannel(final String name) {
        JedisCluster cluster = RedisService.cluster.getOrSet(() -> {
            Set<HostAndPort> hosts = new HashSet<>(options.getClusters());
            GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
            config.setMinIdle(options.getMinIdle());

            config.setMaxIdle(options.getMaxIdle());
            config.setMaxTotal(options.getMaxConnections());

            DefaultJedisClientConfig.Builder cfb = DefaultJedisClientConfig.builder();
            if (options.getUsername() != null) {
                cfb.user(options.getUsername());
            }
            if (options.getPassword() != null) {
                cfb.password(options.getPassword());
            }
            cfb.ssl(options.isSsl());

            JedisClientConfig jedisClientConfig = cfb.build();
            return new JedisCluster(hosts, jedisClientConfig, 5, config);
        });

        return channel.computeIfAbsent(name, (ch) -> new RedisChannel(cluster, name));
    }
}
