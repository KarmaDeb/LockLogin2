package es.karmadev.locklogin.common.api.server.channel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
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

    public SPacket(final Module sender, final JsonElement element) {
        this.sender = sender;
        Gson gson = new GsonBuilder().create();
        raw_data = gson.toJson(element).getBytes(StandardCharsets.UTF_8);
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
