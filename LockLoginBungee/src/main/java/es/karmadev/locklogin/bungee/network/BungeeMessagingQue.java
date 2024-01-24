package es.karmadev.locklogin.bungee.network;

import es.karmadev.api.collect.model.PriorityCollection;
import es.karmadev.api.collect.priority.PriorityArray;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.packet.frame.FrameBuilder;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannelQue;
import es.karmadev.locklogin.api.network.server.packet.NetworkPacket;
import es.karmadev.locklogin.bungee.LockLoginBungee;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the BungeeCord messaging que
 */
public class BungeeMessagingQue implements NetworkChannelQue {

    private final static LockLoginBungee plugin = (LockLoginBungee) CurrentPlugin.getPlugin();

    private final PriorityCollection<NetworkPacket> packets = new PriorityArray<>(NetworkPacket.class);
    private final Map<String, FrameBuilder> frames = new ConcurrentHashMap<>();

    private NetworkPacket current;

    /**
     * Initialize the messaging que
     */
    BungeeMessagingQue(final BungeeChannel channel) {
        String tag = channel.getChannel();
    }

    /**
     * Append a packet
     *
     * @param packet the packet to append
     * @throws SecurityException if there's no module trying to send the packet
     */
    @Override
    public void appendPacket(final NetworkPacket packet) throws SecurityException {
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
            packets.consume();
            current = null;
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

    private static boolean compare(final byte[] b1, final byte[] b2) {
        String b1Hex = digest(b1);
        String b2Hex = digest(b2);

        return b1Hex.equals(b2Hex);
    }

    private static String digest(final byte[] b) {
        try {
            MessageDigest hasher = MessageDigest.getInstance("md5");
            hasher.update(b);

            byte[] result = hasher.digest();
            StringBuilder hexBuilder = new StringBuilder();
            for (byte data : result) {
                int i = Byte.toUnsignedInt(data);
                hexBuilder.append(Integer.toString(i, 16));
            }

            return hexBuilder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}
