package es.karmadev.locklogin.spigot.api;

import org.bukkit.entity.Player;

/**
 * Represents a payload packet
 * wrapper for sending custom payloads
 * between plugin instances using NMS
 */
public abstract class PayloadPacket {

    protected final byte[] data;
    protected final String namespace;

    /**
     * Initialize the payload packet
     *
     * @param data the packet data
     * @param namespace the packet namespace
     */
    public PayloadPacket(final String namespace, final byte[] data) {
        this.data = data;
        this.namespace = namespace;
    }

    /**
     * Create a new payload packet
     *
     * @return the packet data
     */
    public final byte[] getData() {
        return this.data;
    }

    /**
     * Get the namespace
     *
     * @return the namespace
     */
    public final String getNamespace() {
        return this.namespace;
    }

    /**
     * Send the packet
     *
     * @param player the player to send the
     *               packet to
     */
    public abstract void send(final Player player);
}
