package es.karmadev.locklogin.redis;

import es.karmadev.api.object.var.Variable;
import es.karmadev.locklogin.redis.api.RedisService;
import es.karmadev.locklogin.redis.network.RedisChannel;
import es.karmadev.locklogin.redis.api.options.RedisClusterOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the redis service. This service must be
 * provided by a valid {@link es.karmadev.locklogin.api.plugin.service.ServiceProvider}, for example,
 * the default implementation ({@link RedisServiceProvider})
 */
public class DefaultRedisService implements RedisService {

    private final RedisClusterOptions options;
    private final Map<String, RedisChannel> channel = new ConcurrentHashMap<>();

    private final static Variable<JedisCluster> cluster = Variable.notNull(null);

    /**
     * Create the redis service
     */
    protected DefaultRedisService(final RedisClusterOptions options) {
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
    @Override
    public CompletableFuture<RedisChannel> createChannel(final String name) {
        JedisCluster cluster = DefaultRedisService.cluster.getOrSet(() -> {
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

        RedisChannel existing = channel.get(name);
        if (existing != null) {
            return CompletableFuture.completedFuture(existing);
        }

        CompletableFuture<RedisChannel> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            RedisChannel c = new RedisChannel(cluster, name);
            //Cluster subscription must be done asynchronously
            future.complete(c);
        });

        return future;
    }

    /**
     * Get if the service is connected
     * to the redis server
     *
     * @return if the service is connected
     */
    @Override
    public boolean isConnected() {
        return cluster.getReference().isSet();
    }
}
