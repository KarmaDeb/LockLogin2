package es.karmadev.locklogin.bungee.packet;

import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.strings.StringOptions;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.packet.ComPacket;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.bungee.LockLoginBungee;
import es.karmadev.locklogin.common.util.Task;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.protocol.DefinedPacket;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Packet data handler, mostly used to
 * validate packets
 */
public class PacketDataHandler {

    private final static LockLoginBungee plugin = (LockLoginBungee) CurrentPlugin.getPlugin();
    private final static Map<String, PublicKey> keys = new ConcurrentHashMap<>();
    private final static ConcurrentMap<Server, Map<String, ComPacket>> packetData = new ConcurrentHashMap<>();
    private final static ConcurrentMap<ComPacket, Task<IncomingPacket>> packetTasks = new ConcurrentHashMap<>();

    /**
     * Emit a packet
     *
     * @param server the target server
     * @param packet the packet
     */
    public static Task<IncomingPacket> emitPacket(final Server server, final OutgoingPacket packet) {
        String packetId = StringUtils.generateString(16, StringOptions.LOWERCASE);
        Map<String, ComPacket> ids = packetData.computeIfAbsent(server, (data) -> new ConcurrentHashMap<>());

        Task<IncomingPacket> task = new Task<IncomingPacket>() {

            Consumer<IncomingPacket> consumer = (p) -> {};

            @Override
            public void apply(final IncomingPacket object) {
                consumer.accept(object);
            }

            @Override
            public void then(final Consumer<IncomingPacket> object) {
                if (object != null) consumer = object;
            }
        };
        packetTasks.put(packet, task);
        if (ids.containsKey(packetId)) {
            emitPacket(server, packet);
            return task;
        }

        ids.put(packetId, packet);
        packetData.put(server, ids);

        DefinedPacket[] bungeePacket = CustomPacket.buildOutgoing(packetId, packet, keys.getOrDefault(server.getInfo().getName(), null));
        for (DefinedPacket dp : bungeePacket) {
            server.unsafe().sendPacket(dp);
        }

        return task;
    }

    public static Task<IncomingPacket> getTask(final ComPacket packet) {
        ComPacket key = null;
        for (ComPacket pack : packetTasks.keySet()) {
            int replyId = 0;
            if (packet instanceof IncomingPacket) {
                replyId = ((IncomingPacket) packet).getInteger("replying");
            }
            if (packet instanceof OutgoingPacket) {
                OutgoingPacket out = (OutgoingPacket) packet;
                JsonObject object = out.build();

                replyId = object.getChild("replying").asNative().getInteger();
            }

            if (pack.id() == replyId) {
                key = pack;
                break;
            }
        }

        if (key != null) {
            return packetTasks.remove(key);
        }

        return null;
    }

    /**
     * Validate a packet
     *
     * @param emitter the server who emitted the
     *                packet
     * @param packetId the received packet id
     * @param packet the received packet
     * @return if the packet is valid
     */
    public static boolean validatePacket(final Server emitter, String packetId, final ComPacket packet) {
        if (packet == null) return false;
        if (packetId.contains(":")) {
            packetId = packetId.split(":")[1]; //0 is the prefix
        }

        Map<String, ComPacket> ids = packetData.computeIfAbsent(emitter, (data) -> new ConcurrentHashMap<>());
        ComPacket stored = ids.remove(packetId);
        if (stored == null) return false;

        int replyId = 0;
        if (packet instanceof IncomingPacket) {
            replyId = ((IncomingPacket) packet).getInteger("replying");
        }
        if (packet instanceof OutgoingPacket) {
            OutgoingPacket out = (OutgoingPacket) packet;
            JsonObject object = out.build();

            replyId = object.getChild("replying").asNative().getInteger();
        }

        return stored.id() == replyId;
    }

    public static boolean tagExists(final String tag) {
        String packetId = null;
        if (tag.contains(":")) {
            packetId = tag.split(":")[1]; //0 is the prefix
        }
        if (packetId == null) return false;

        for (Server server : packetData.keySet()) {
            Map<String, ComPacket> packets = packetData.computeIfAbsent(server, (data) -> new ConcurrentHashMap<>());
            if (packets.containsKey(packetId)) return true;
        }

        return false;
    }

    public static void assignKey(final ServerInfo server, final PublicKey key) {
        keys.put(server.getName(), key);
    }
}
