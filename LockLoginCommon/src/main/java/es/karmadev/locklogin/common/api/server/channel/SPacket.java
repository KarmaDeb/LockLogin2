package es.karmadev.locklogin.common.api.server.channel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.LockLogin;
import es.karmadev.locklogin.api.extension.Module;
import es.karmadev.locklogin.api.network.server.packet.NetworkPacket;
import es.karmadev.locklogin.api.plugin.runtime.LockLoginRuntime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Network packet
 */
public class SPacket implements NetworkPacket {

    private int priority = 0;
    private final Module sender;
    private final byte[] raw_data;

    public SPacket(final byte[] message) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

        Path caller = plugin.runtime().caller();

        sender = plugin.moduleManager().loader().find(caller);
        raw_data = Base64.getEncoder().encode(message);
    }

    public SPacket(final String raw) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

        Path caller = plugin.runtime().caller();

        sender = plugin.moduleManager().loader().find(caller);
        raw_data = Base64.getEncoder().encode(raw.getBytes(StandardCharsets.UTF_8));
    }

    public SPacket(final JsonElement element) {
        LockLogin plugin = CurrentPlugin.getPlugin();
        plugin.runtime().verifyIntegrity(LockLoginRuntime.PLUGIN_AND_MODULES);

        Path caller = plugin.runtime().caller();

        sender = plugin.moduleManager().loader().find(caller);
        Gson gson = new GsonBuilder().create();
        raw_data = gson.toJson(element).getBytes(StandardCharsets.UTF_8);
    }

    public SPacket priority(final int level) {
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
