package es.karmadev.locklogin.api.protocol;

import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;

/**
 * Represents the LockLogin protocol
 */
public interface LockLoginProtocol {

    /**
     * Receive data to the protocol handler
     *
     * @param channel the channel in where the
     *                data has been received
     * @param tag the message tag
     * @param frame the received frame
     * @throws InvalidPacketDataException if the packet full data
     * results on an invalid packet. This might be caused by a MitM
     * attack, or a malformed packet (possibly caused by an outdated
     * plugin)
     */
    void receive(final String channel, final String tag, final PacketFrame frame) throws InvalidPacketDataException;

    /**
     * Write data into the protocol
     *
     * @param channel the channel name
     * @param tag the message tag
     * @param data the data to write
     */
    void write(final String channel, final String tag, final OutgoingPacket data);

    /**
     * Forget all the data from a channel. This does the
     * same effect as if the other part sent us a {@link es.karmadev.locklogin.api.network.communication.data.DataType#CHANNEL_CLOSE}
     *
     * @param channel the channel to forget
     */
    void forget(final String channel);

    /**
     * Get the protocol encoded secret
     * key
     *
     * @return the protocol encoded secret
     */
    byte[] getEncodedSecret();
}
