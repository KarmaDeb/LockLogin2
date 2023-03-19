package es.karmadev.locklogin.api.network.communication;

import es.karmadev.locklogin.api.network.communication.data.Channel;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.plugin.service.WebService;

/**
 * LockLogin communication service
 */
public interface CommunicationService extends WebService {

    /**
     * Subscribe to a channel
     *
     * @param channels the channels to subscribe
     */
    void subscribe(final Channel... channels);

    /**
     * Unsubscribe a channel
     *
     * @param channels the channels to unsubscribe
     */
    void unsubscribe(final Channel... channels);

    /**
     * When a packet has been sent
     *
     * @param channel the receiving channel
     * @param packet the packet
     */
    void onPacketReceive(final Channel channel, final IncomingPacket packet);

    /**
     * When a packet has been sent
     *
     * @param channel the packet channel
     * @param packet the packet
     * @return if the packet will be sent
     */
    boolean onPacketSend(final Channel channel, final IncomingPacket packet);

    /**
     * Send a packet
     *
     * @param channel the channel to send the packet
     *                on
     * @param packet the packet to send
     * @param priority the packet priority
     */
    void sendPacket(final Channel channel, final OutgoingPacket packet, final long priority);
}
