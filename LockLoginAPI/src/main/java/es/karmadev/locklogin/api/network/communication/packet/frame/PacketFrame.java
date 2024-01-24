package es.karmadev.locklogin.api.network.communication.packet.frame;

import es.karmadev.locklogin.api.network.communication.packet.ComPacket;

import java.util.UUID;

/**
 * The frame of a packet
 */
public interface PacketFrame extends ComPacket {

    /**
     * Get the frame unique id
     *
     * @return the frame unique id
     */
    UUID uniqueId();

    /**
     * Get this frame position
     *
     * @return the frame position
     */
    int position();

    /**
     * Get the amount of frames for this
     * packet frame
     *
     * @return the amount of frames
     */
    int frames();

    /**
     * Get the frame length
     *
     * @return the frame length
     */
    int length();

    /**
     * Read the frame data
     *
     * @param index the start index
     * @param output the output data
     */
    void read(final byte[] output, final int index);
}
