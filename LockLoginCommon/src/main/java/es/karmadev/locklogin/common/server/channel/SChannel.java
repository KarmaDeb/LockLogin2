package es.karmadev.locklogin.common.server.channel;

import es.karmadev.locklogin.api.network.server.packet.NetworkChannel;
import es.karmadev.locklogin.api.network.server.packet.NetworkPacket;

public class SChannel implements NetworkChannel {

    /**
     * Append a packet
     *
     * @param packet the packet to append
     * @throws SecurityException if there's no module trying to send the packet
     */
    @Override
    public void appendPacket(final NetworkPacket packet) throws SecurityException {

    }

    /**
     * Get the next packet
     *
     * @return the next packet
     */
    @Override
    public NetworkPacket nextPacket() {
        return null;
    }

    /**
     * Get the previous packet
     *
     * @return the previous packet
     */
    @Override
    public NetworkPacket previousPacket() {
        return null;
    }

    /**
     * Get if the queue is processing a packet
     *
     * @return if the queue is occupied
     */
    @Override
    public boolean processing() {
        return false;
    }

    /**
     * Cancel the current packet processing, and
     * move it to the latest in the queue
     */
    @Override
    public void shiftPacket() {

    }

    /**
     * Consume the current packet
     */
    @Override
    public void consumePacket() {

    }

    /**
     * Cancel the current packet processing, and
     * do nothing to it
     */
    @Override
    public void cancelPacket() {

    }
}
