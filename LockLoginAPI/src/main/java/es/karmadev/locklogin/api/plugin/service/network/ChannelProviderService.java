package es.karmadev.locklogin.api.plugin.service.network;

import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import es.karmadev.locklogin.api.plugin.service.PluginService;
import es.karmadev.locklogin.api.task.FutureTask;

import java.util.Collection;

/**
 * Represents a channel provider service. This
 * service is responsible for creating {@link es.karmadev.locklogin.api.network.communication.packet.NetworkChannel channels},
 * those channels are the ones who manage the
 * network communication.
 */
public abstract class ChannelProviderService implements PluginService {

    /**
     * Get the service name
     *
     * @return the service name
     */
    public final String name() {
        return "plugin-messaging";
    }

    /**
     * Get a collection of the registered
     * channels from this provider.
     *
     * @return the provider created channels
     */
    public abstract Collection<? extends NetworkChannel> getChannels();

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
    public abstract FutureTask<? extends NetworkChannel> getChannel(final String channelName);

    /**
     * Destroy a channel
     *
     * @param channelName the channel name to destroy
     * @return if the channel was destroyed
     */
    public abstract boolean destroyChannel(final String channelName);

    /**
     * Implementations may relay on 3rd party services
     * such as redis to create channels. This method verifies
     * that the provider is connected to that service
     *
     * @return if the provider is connected to all the
     * third party services it depends on
     */
    public abstract boolean isConnected();
}
