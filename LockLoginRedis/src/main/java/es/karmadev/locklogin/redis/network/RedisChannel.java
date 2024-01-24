package es.karmadev.locklogin.redis.network;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import es.karmadev.locklogin.api.network.communication.packet.listener.NetworkListener;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.NetworkEvent;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannelQue;
import es.karmadev.locklogin.common.util.generic.GenericHandlerList;
import redis.clients.jedis.JedisCluster;

public class RedisChannel implements NetworkChannel {

    private final String channel;
    private final RedisMessageQue que;

    private final GenericHandlerList handlerList = new GenericHandlerList();

    public RedisChannel(final JedisCluster cluster, final String channel) {
        this.channel = channel;
        this.que = new RedisMessageQue(cluster, this);
    }

    /**
     * Get the channel name this
     * network channel works at
     *
     * @return the channel name
     */
    @Override
    public String getChannel() {
        return channel;
    }

    /**
     * Get the channel que
     *
     * @return the channel que
     */
    @Override
    public NetworkChannelQue getProcessingQue() {
        return que;
    }

    /**
     * Handle a network event
     *
     * @param event the event to handle
     */
    @Override
    public void handle(final NetworkEvent event) {
        handlerList.handle(event);
    }

    /**
     * Add a listener
     *
     * @param module   the module listener owner
     * @param listener the listener
     */
    @Override
    public void addListener(final Module module, final NetworkListener listener) {
        this.handlerList.addListeners(module, listener);
    }

    /**
     * Remove a single listener
     *
     * @param listener the listener to remove
     */
    @Override
    public void removeListener(final NetworkListener listener) {
        this.handlerList.removeListener(listener);
    }

    /**
     * Remove all the listeners registered
     * on the module
     *
     * @param module the module to remove listeners
     *               from
     */
    @Override
    public void removeListeners(final Module module) {
        this.handlerList.removeListeners(module);
    }
}

