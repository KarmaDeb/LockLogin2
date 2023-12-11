package es.karmadev.locklogin.redis.api.options;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.HostAndPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a redis cluster options
 */
@Builder @Getter
public class RedisClusterOptions {

    @Getter
    private List<HostAndPort> clusters;

    @Builder.Default
    private int maxIdle = 10;
    @Builder.Default
    private int minIdle = 5;
    @Builder.Default
    private int maxConnections = 20;

    @Nullable
    private final String username;
    @Nullable
    private final String password;

    @Builder.Default
    private boolean ssl = false;

    public static class RedisClusterOptionsBuilder {

        private RedisClusterOptionsBuilder clusters(final List<HostAndPort> hostAndPorts) {
            this.clusters = hostAndPorts;
            return this;
        }

        public RedisClusterOptionsBuilder withCluster(final HostAndPort hostAndPort) {
            if (this.clusters == null) this.clusters = new ArrayList<>();

            this.clusters.add(hostAndPort);
            return this;
        }
    }
}
