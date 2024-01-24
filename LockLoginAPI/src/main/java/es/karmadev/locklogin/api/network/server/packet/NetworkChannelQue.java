package es.karmadev.locklogin.api.network.server.packet;

/**
 * Packet processing queue
 */
public interface NetworkChannelQue {

    /**
     * Append a packet
     *
     * @param packet the packet to append
     * @throws SecurityException if there's no module trying to send the packet
     */
    void appendPacket(final NetworkPacket packet) throws SecurityException;

    /**
     * Get the next packet
     *
     * @return the next packet
     */
    NetworkPacket nextPacket();

    /**
     * Get the previous packet
     *
     * @return the previous packet
     */
    NetworkPacket previousPacket();

    /**
     * Get if the queue is processing a packet
     *
     * @return if the queue is occupied
     */
    boolean processing();

    /**
     * Cancel the current packet processing, and
     * move it to the latest in the queue
     */
    void shiftPacket();

    /**
     * Consume the current packet
     */
    void consumePacket();

    /**
     * Cancel the current packet processing, and
     * do nothing to it
     */
    void cancelPacket();
}
