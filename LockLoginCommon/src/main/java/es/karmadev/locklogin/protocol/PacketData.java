package es.karmadev.locklogin.protocol;

import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.FrameBuilder;
import es.karmadev.locklogin.common.api.packet.frame.CFrameBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an internal usage-only
 * packet data
 */
class PacketData {

    private final Map<String, OutgoingPacket> packetMap = new ConcurrentHashMap<>();
    private final Map<String, FrameBuilder> frameMap = new ConcurrentHashMap<>();

    public void assign(final String tag, final OutgoingPacket packet) {
        this.packetMap.put(tag, packet);
    }

    public OutgoingPacket retrieve(final String tag) {
        return this.packetMap.remove(tag);
    }

    public FrameBuilder getFrameBuilder(final String tag) {
        return this.frameMap.computeIfAbsent(tag, (fb) -> new CFrameBuilder());
    }

    public void revokeBuilder(final String tag) {
        this.frameMap.remove(tag);
    }
}
