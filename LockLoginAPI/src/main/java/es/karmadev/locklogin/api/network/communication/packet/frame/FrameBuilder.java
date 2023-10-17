package es.karmadev.locklogin.api.network.communication.packet.frame;

import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;

/**
 * Packet frame builder
 */
public interface FrameBuilder {

    /**
     * Append a frame to the builder
     *
     * @param frame the frame to append
     */
    void append(final PacketFrame frame);

    /**
     * Build the packet from the frames
     *
     * @return the packet
     * @throws InvalidPacketDataException if the packet is not a valid packet
     */
    byte[] build() throws InvalidPacketDataException;

    /**
     * Split the packet into packet frames
     *
     * @param packet the packet to split
     * @param rawPacket the packet to split
     * @return the packet frames
     */
    PacketFrame[] split(final OutgoingPacket packet, final byte[] rawPacket);
}
