package es.karmadev.locklogin.redis.network;

import es.karmadev.api.collect.model.PriorityCollection;
import es.karmadev.api.collect.priority.PriorityArray;
import es.karmadev.locklogin.api.extension.module.Module;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.NetworkChannel;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.PacketReceiveEvent;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannelQue;
import es.karmadev.locklogin.api.network.server.packet.NetworkPacket;
import es.karmadev.locklogin.common.api.packet.CInPacket;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.JedisCluster;

import java.nio.charset.StandardCharsets;

/**
 * Represents a redis message que
 */
public class RedisMessageQue implements NetworkChannelQue {

    private final NetworkChannel channel;

    private final PriorityCollection<NetworkPacket> packets = new PriorityArray<>(NetworkPacket.class);

    private NetworkPacket current;

    public RedisMessageQue(final JedisCluster cluster, final NetworkChannel channel) {
        this.channel = channel;

        cluster.subscribe(new BinaryJedisPubSub() {
            @Override
            public void onMessage(final byte[] channel, final byte[] message) {
                String rawMessage = new String(message);

                try {
                    CInPacket packet = new CInPacket(rawMessage);
                    PacketReceiveEvent event = new PacketReceiveEvent(RedisMessageQue.this.channel, packet);

                    RedisMessageQue.this.channel.handle(event);
                } catch (InvalidPacketDataException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, channel.getChannel().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Append a packet
     *
     * @param packet the packet to append
     * @throws SecurityException if there's no module trying to send the packet
     */
    @Override
    public void appendPacket(final NetworkPacket packet) throws SecurityException {
        Module sender = packet.sender();
        if (sender == null || !sender.isEnabled()) throw new SecurityException("Cannot append packet from an invalid module");

        packets.add(packet, packet.priority());
    }

    /**
     * Get the next packet
     *
     * @return the next packet
     */
    @Override
    public NetworkPacket nextPacket() {
        if (packets.isEmpty()) return null;

        current = packets.next();
        return current;
    }

    /**
     * Get the previous packet
     *
     * @return the previous packet
     */
    @Override
    public NetworkPacket previousPacket() {
        if (packets.size() < 2) return null;

        current = packets.previous();
        return current;
    }

    /**
     * Get if the queue is processing a packet
     *
     * @return if the queue is occupied
     */
    @Override
    public boolean processing() {
        return current != null;
    }

    /**
     * Cancel the current packet processing, and
     * move it to the latest in the queue
     */
    @Override
    public void shiftPacket() {
        if (current != null) {
            packets.consume();
            packets.add(current, current.priority());

            current = null;
        }
    }

    /**
     * Consume the current packet
     */
    @Override
    public void consumePacket() {
        if (current != null) {
            current = null;
            packets.consume();
        }
    }

    /**
     * Cancel the current packet processing, and
     * do nothing to it
     */
    @Override
    public void cancelPacket() {
        current = null;
    }
}
