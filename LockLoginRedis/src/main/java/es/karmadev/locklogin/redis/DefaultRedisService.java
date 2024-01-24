package es.karmadev.locklogin.redis;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.object.var.Variable;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import es.karmadev.locklogin.api.plugin.service.network.ChannelProviderService;
import es.karmadev.locklogin.api.task.FutureTask;
import es.karmadev.locklogin.redis.network.RedisChannel;
import es.karmadev.locklogin.redis.options.RedisClusterOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.util.Collection;
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
public class DefaultRedisService extends ChannelProviderService {

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
     * Get a collection of the registered
     * channels from this provider.
     *
     * @return the provider created channels
     */
    @Override
    public Collection<? extends NetworkChannel> getChannels() {
        return channel.values();
    }

    /**
     * Create a new channel on the redis
     * network
     *
     * @param name the channel name
     * @return the redis channel
     */
    @Override
    public FutureTask<RedisChannel> getChannel(final String name) {
        if (name == null) return FutureTask.cancelledFuture();

        String normalized = StringUtils.toSnakeCase(name);
        if (ObjectUtils.isNullOrEmpty(normalized)) return FutureTask.cancelledFuture();

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

        RedisChannel existing = channel.get(normalized);
        if (existing != null) {
            return FutureTask.completedFuture(existing);
        }

        FutureTask<RedisChannel> future = new FutureTask<>();
        CompletableFuture.runAsync(() -> {
            RedisChannel c = new RedisChannel(cluster, normalized);
            channel.put(normalized, c);

            //Cluster subscription must be done asynchronously
            future.complete(c);
        });

        return future;
    }

    /**
     * Destroy a channel
     *
     * @param channelName the channel name to destroy
     * @return if the channel was destroyed
     */
    @Override
    public boolean destroyChannel(final String channelName) {
        if (channelName == null) return false;
        String normalized = StringUtils.toSnakeCase(channelName);
        if (ObjectUtils.isNullOrEmpty(normalized)) return false;

        return channel.remove(normalized) != null;
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