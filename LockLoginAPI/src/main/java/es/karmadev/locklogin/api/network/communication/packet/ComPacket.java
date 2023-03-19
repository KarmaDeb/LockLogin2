package es.karmadev.locklogin.api.network.communication.packet;

import java.time.Instant;

/**
 * LockLogin json packet
 */
public interface ComPacket {

    /**
     * Get the packet ID
     *
     * @return the packet id
     */
    int id();

    /**
     * Get the packet timestamp
     *
     * @return the packet timestamp
     */
    Instant timestamp();
}
