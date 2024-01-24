package es.karmadev.locklogin.common.api.server.channel;

import es.karmadev.api.kson.JsonInstance;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.server.packet.NetworkPacket;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Network packet
 */
@SuppressWarnings("unused")
public class SPacket implements NetworkPacket {

    private int priority = 0;
    private final Module sender;
    private final byte[] raw_data;

    public SPacket(final Module sender, final byte[] message) {
        this.sender = sender;
        raw_data = Base64.getEncoder().encode(message);
    }

    public SPacket(final Module sender, final String raw) {
        this.sender = sender;
        raw_data = Base64.getEncoder().encode(raw.getBytes(StandardCharsets.UTF_8));
    }

    public SPacket(final Module sender, final JsonInstance element) {
        this.sender = sender;
        raw_data = element.toString(false).getBytes();
    }

    public SPacket priority(final int level) {
        priority = level;
        return this;
    }

    /**
     * Get the packet priority
     *
     * @return the packet priority
     */
    @Override
    public int priority() {
        return priority;
    }

    /**
     * Get the module that is trying to send the packet
     *
     * @return the module sending the packet
     */
    @Override
    public Module sender() {
        return sender;
    }

    /**
     * Get the message
     *
     * @return the message
     */
    @Override
    public byte[] message() {
        return raw_data;
    }
}
