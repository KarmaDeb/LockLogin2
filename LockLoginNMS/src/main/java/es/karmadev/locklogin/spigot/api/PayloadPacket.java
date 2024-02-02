package es.karmadev.locklogin.spigot.api;

import org.bukkit.entity.Player;

/**
 * Represents a payload packet
 * wrapper for sending custom payloads
 * between plugin instances using NMS
 */
public abstract class PayloadPacket {

    protected final byte[] data;

    /**
     * Initialize the payload packet
     *
     * @param data the packet data
     */
    public PayloadPacket(final byte[] data) {
        this.data = data;
    }

    /**
     * Create a new payload packet
     *
     * @return the packet data
     */
    public final byte[] getData() {
        return this.data;
    }

    public abstract void send(final Player player);
}
