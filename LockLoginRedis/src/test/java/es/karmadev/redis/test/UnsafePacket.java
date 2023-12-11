package es.karmadev.redis.test;

import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.server.packet.NetworkPacket;

import java.util.Base64;

public class UnsafePacket implements NetworkPacket {

    private final byte[] raw_data;

    public UnsafePacket(final byte[] message) {
        raw_data = Base64.getEncoder().encode(message);
    }

    /**
     * Get the packet priority
     *
     * @return the packet priority
     */
    @Override
    public int priority() {
        return 0;
    }

    /**
     * Get the module that is trying to send the packet
     *
     * @return the module sending the packet
     */
    @Override
    public Module sender() {
        return null;
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
