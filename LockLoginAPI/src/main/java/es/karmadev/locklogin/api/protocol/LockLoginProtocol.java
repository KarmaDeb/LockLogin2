package es.karmadev.locklogin.api.protocol;

import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;

/**
 * Represents the LockLogin protocol
 */
public interface LockLoginProtocol {

    /**
     * Get the protocol channel
     *
     * @return the channel
     */
    String getChannel();

    /**
     * Set the protocol channel if
     * it's not defined
     *
     * @param newChannel the new channel
     */
    void setChannel(final String newChannel);

    /**
     * Receive data to the protocol handler
     *
     * @param frame the received frame
     * @throws InvalidPacketDataException if the packet full data
     * results on an invalid packet. This might be caused by a MitM
     * attack, or a malformed packet (possibly caused by an outdated
     * plugin)
     */
    void receive(final String tag, final PacketFrame frame) throws InvalidPacketDataException;

    /**
     * Write data into the protocol
     *
     * @param data the data to write
     */
    void write(final String tag, final OutgoingPacket data);

    /**
     * Get the protocol encoded secret
     * key
     *
     * @return the protocol encoded secret
     */
    byte[] getEncodedSecret();
}
