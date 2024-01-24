package es.karmadev.locklogin.bungee.network;

import es.karmadev.api.object.ObjectUtils;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import es.karmadev.locklogin.api.plugin.service.network.ChannelProviderService;
import es.karmadev.locklogin.api.task.FutureTask;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BungeeMessagingService extends ChannelProviderService {

    private final Map<String, BungeeChannel> channelMap = new ConcurrentHashMap<>();

    /**
     * Get if this service must be
     * obtained via a service provider
     *
     * @return if the service depends
     * on a service provider
     */
    @Override
    public boolean useProvider() {
        return false; //This doesn't require a provider
    }

    /**
     * Get a collection of the registered
     * channels from this provider.
     *
     * @return the provider created channels
     */
    @Override
    public Collection<? extends NetworkChannel> getChannels() {
        return channelMap.values();
    }

    /**
     * Get a channel
     *
     * @param channelName the channel name
     * @return the channel. Implementations should
     * return an existing channel if there was one
     * created with the same name previously. As that
     * what the plugin internally expects from the
     * channel
     */
    @Override
    public FutureTask<? extends NetworkChannel> getChannel(final String channelName) {
        if (channelName == null) return FutureTask.cancelledFuture();

        String normalized = StringUtils.toSnakeCase(channelName);
        if (ObjectUtils.isNullOrEmpty(normalized)) return FutureTask.cancelledFuture();

        return FutureTask.completedFuture(
                channelMap.computeIfAbsent(normalized, (ch) -> new BungeeChannel(normalized)));
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

        return channelMap.remove(normalized) != null;
    }

    /**
     * Implementations may relay on 3rd party services
     * such as redis to create channels. This method verifies
     * that the provider is connected to that service
     *
     * @return if the provider is connected to all the
     * third party services it depends on
     */
    @Override
    public boolean isConnected(){
        return true; //We are always connected when using this
    }
}
