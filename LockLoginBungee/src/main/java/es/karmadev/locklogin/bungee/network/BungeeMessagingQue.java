package es.karmadev.locklogin.bungee.network;

import es.karmadev.api.collect.model.PriorityCollection;
import es.karmadev.api.collect.priority.PriorityArray;
import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.FrameBuilder;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;
import es.karmadev.locklogin.api.network.communication.packet.listener.event.PacketReceiveEvent;
import es.karmadev.locklogin.api.network.server.packet.NetworkChannelQue;
import es.karmadev.locklogin.api.network.server.packet.NetworkPacket;
import es.karmadev.locklogin.bungee.LockLoginBungee;
import es.karmadev.locklogin.bungee.packet.PacketDataHandler;
import es.karmadev.locklogin.common.api.packet.CInPacket;
import es.karmadev.locklogin.common.api.packet.frame.CFrameBuilder;
import net.md_5.bungee.api.ProxyServer;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
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

        plugin.plugin().addChannelListener(tag, (server, rawData) -> {
            if (rawData == null) return;
            String rawPacketData = new String(rawData, StandardCharsets.UTF_8);

            Object packetObject = StringUtils.load(rawPacketData).orElse(null);
            if (packetObject instanceof PacketFrame) {
                PacketFrame frame = (PacketFrame) packetObject;
                FrameBuilder builder = frames.computeIfAbsent(tag, (b) -> new CFrameBuilder((rawPacket) -> {
                    KeyPair pair = plugin.getCommunicationKeys();
                    try {
                        Cipher cipher = Cipher.getInstance("RSA");
                        cipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
                        return cipher.doFinal(rawPacket);
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                             IllegalBlockSizeException |
                             BadPaddingException ex) {
                        ex.printStackTrace();
                    }

                    return null;
                }));
                builder.append(frame);

                int position = frame.position();
                int max = frame.frames();

                if (position == max) {
                    ProxyServer.getInstance().getScheduler().runAsync(plugin.plugin(), () -> {
                        try {
                            byte[] rawPacket = builder.build();

                            String raw = new String(rawPacket, StandardCharsets.UTF_8);
                            Object packet = StringUtils.load(raw).orElse(null);

                            if (packet instanceof OutgoingPacket) {
                                OutgoingPacket out = (OutgoingPacket) packet;
                                DataType type = out.getType();

                                if (!type.equals(DataType.HELLO)) {
                                    JsonObject object = out.build();
                                    if (object.hasChild(tag)) {
                                        byte[] sharedBytes = Base64.getDecoder().decode(object.getChild(tag)
                                                .asNative().getAsString());
                                        byte[] currentBytes = plugin.getSharedSecret().getEncoded();

                                        if (!compare(sharedBytes, currentBytes)) {
                                            plugin.err("Received invalid packet (invalid shared)");
                                            return;
                                        }
                                    } else {
                                        plugin.err("Received unverified packet (missing shared)");
                                        return;
                                    }
                                }

                                if (PacketDataHandler.validatePacket(server, tag, out)) {
                                    JsonObject object = out.build();
                                    object.put("server", server.getInfo().getName());

                                    String rawJson = object.toString(false);

                                    IncomingPacket incoming = new CInPacket(rawJson);
                                    channel.handle(new PacketReceiveEvent(channel, incoming));
                                }
                            }
                        } catch (InvalidPacketDataException ex) {
                            plugin.log(ex, "Failed to handle packet under tag {0}", tag);
                        }
                    });
                }
            }
        });
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
