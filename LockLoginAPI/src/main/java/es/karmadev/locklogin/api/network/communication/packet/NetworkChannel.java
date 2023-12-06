package es.karmadev.locklogin.api.network.communication.packet;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.communication.packet.listener.NetworkListener;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.NetworkEvent;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannelQue;

/**
 * Represents a network channel
 */
public interface NetworkChannel {

    /**
     * Get the channel name this
     * network channel works at
     *
     * @return the channel name
     */
    String getChannel();

    /**
     * Get the channel que
     *
     * @return the channel que
     */
    NetworkChannelQue getProcessingQue();

    /**
     * Handle a network event
     *
     * @param event the event to handle
     */
    void handle(final NetworkEvent event);

    /**
     * Add a listener
     *
     * @param module the module listener owner
     * @param listener the listener
     */
    void addListener(final Module module, final NetworkListener listener);

    /**
     * Remove a single listener
     *
     * @param listener the listener to remove
     */
    void removeListener(final NetworkListener listener);

    /**
     * Remove all the listeners registered
     * on the module
     *
     * @param module the module to remove listeners
     *               from
     */
    void removeListeners(final Module module);
}
