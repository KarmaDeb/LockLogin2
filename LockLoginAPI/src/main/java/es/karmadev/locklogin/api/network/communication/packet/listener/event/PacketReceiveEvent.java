package es.karmadev.locklogin.api.network.communication.packet.listener.event;

import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import lombok.Getter;

/**
 * Represents a network event
 */
@Getter
public class PacketReceiveEvent extends NetworkEvent {

    private final IncomingPacket packet;

    /**
     * Initialize the network receiving packet
     *
     * @param channel the channel
     * @param packet the packet
     */
    public PacketReceiveEvent(final NetworkChannel channel, final IncomingPacket packet) {
        super(channel);
        this.packet = packet;
    }
}
