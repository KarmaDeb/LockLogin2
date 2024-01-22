package es.karmadev.locklogin.bungee.packet;

import es.karmadev.api.kson.JsonObject;
import es.karmadev.api.strings.StringOptions;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.data.DataType;
import es.karmadev.locklogin.api.network.communication.exception.InvalidPacketDataException;
import es.karmadev.locklogin.api.network.communication.packet.IncomingPacket;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.api.network.communication.packet.frame.FrameBuilder;
import es.karmadev.locklogin.api.network.communication.packet.frame.PacketFrame;
import es.karmadev.locklogin.bungee.LockLoginBungee;
import es.karmadev.locklogin.common.api.packet.CInPacket;
import es.karmadev.locklogin.common.api.packet.frame.CFrameBuilder;
import es.karmadev.locklogin.common.api.packet.frame.CFramePacket;
import es.karmadev.locklogin.common.util.Task;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.PluginMessage;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom packet builder
 */
public class CustomPacket {

    private final static LockLoginBungee plugin = (LockLoginBungee) CurrentPlugin.getPlugin();
    private final static Map<String, FrameBuilder> frames = new ConcurrentHashMap<>();

    /**
     * Handle the packet from the event
     *
     * @param event the event
     */
    public static void handle(final PluginMessageEvent event) {
        String tag = event.getTag();
        System.out.println(tag);

        if (PacketDataHandler.tagExists(tag)) {
            System.out.println("Tag exists!");
            Server server = (Server) event.getSender();

            byte[] rawData = event.getData();
            if (rawData == null) return;

            String rawPacketData = new String(rawData, StandardCharsets.UTF_8);
            Object packetObject = StringUtils.load(rawPacketData).orElse(null);

            if (packetObject != null) {
                System.out.println(packetObject.getClass().getSimpleName());
            }

            if (packetObject instanceof PacketFrame) {
                /*
                We actually only accept packet frames, any other type of
                packet we receive will be discarded as "invalid", even though
                we could perfectly read it, it has not been "verified" by being
                sent encrypted through a packet frame
                */

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
                    System.out.println("Received final frame");
                    ProxyServer.getInstance().getScheduler().runAsync(plugin.plugin(), () -> {
                        try {
                            byte[] rawPacket = builder.build();

                            String raw = new String(rawPacket, StandardCharsets.UTF_8);
                            Object packet = StringUtils.load(raw).orElse(null);

                            if (packet != null) {
                                System.out.println(packet.getClass().getSimpleName());
                            }

                            if (packet instanceof OutgoingPacket) {
                                OutgoingPacket out = (OutgoingPacket) packet;
                                DataType type = out.getType();

                                if (!type.equals(DataType.HELLO)) {
                                    JsonObject object = out.build();
                                    if (object.hasChild(tag)) {
                                        byte[] sharedBytes = Base64.getDecoder().decode(object.getChild(tag).asNative().getAsString());
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

                                System.out.println("Validating packet...");
                                if (PacketDataHandler.validatePacket(server, tag, out)) {
                                    System.out.println("Packet validated!");
                                    JsonObject object = out.build();
                                    object.put("server", server.getInfo().getName());

                                    String rawJson = object.toString(false);

                                    IncomingPacket incoming = new CInPacket(rawJson);
                                    plugin.onReceive(incoming);

                                    /*NetworkChannel channel = plugin.getChannel(tag);

                                    if (channel == null) {
                                        throw new IllegalStateException("Received a packet from unregistered channel: " + tag);
                                    }

                                    channel.handle(
                                            new PacketReceiveEvent(channel, incoming)
                                    );*/


                                    Task<IncomingPacket> task = PacketDataHandler.getTask(incoming);
                                    System.out.println(task);
                                    if (task != null) {
                                        task.apply(incoming);
                                    }
                                } else {
                                    System.out.println("Failed to validate packet");
                                }
                            }
                        } catch (InvalidPacketDataException ex) {
                            plugin.log(ex, "Failed to handle packet under tag {0}", tag);
                        }
                    });
                }
            }
        }
    }

    /**
     * Build an incoming packet
     *
     * @param id       the packet id
     * @param incoming the outgoing packet
     * @return the created packet
     * @deprecated moved logic to another place
     */
    @Deprecated
    public static DefinedPacket buildIncoming(final String id, final IncomingPacket incoming) {
        String rawObject = StringUtils.serialize(incoming);
        byte[] data = rawObject.getBytes(StandardCharsets.UTF_8);

        return new PluginMessage(id, data, true);
    }

    /**
     * Build an outgoing packet
     *
     * @param id       the packet id
     * @param outgoing the outgoing packet
     * @param key      the key to use
     * @return the created packet
     */
    public static DefinedPacket[] buildOutgoing(final String id, final OutgoingPacket outgoing, final PublicKey key) {
        List<DefinedPacket> packetList = new ArrayList<>();

        String rawObject = StringUtils.serialize(outgoing);
        byte[] packetData = rawObject.getBytes(StandardCharsets.UTF_8);
        String prefix = StringUtils.generateString(4, StringOptions.LOWERCASE);

        if (outgoing.getType().equals(DataType.HELLO)) { //We don't encrypt HELLO packets
            DefinedPacket message = new PluginMessage(prefix + ":" + id, packetData, true);
            packetList.add(message);
        } else {
            List<byte[]> dataToEncrypt = new ArrayList<>();

            if (packetData.length <= 200) {
                dataToEncrypt.add(packetData);
            } else {
                int blocks = packetData.length / 200;
                int remainingBytes = packetData.length % 200;

                for (int i = 0; i < blocks; i++) {
                    byte[] block = new byte[200];
                    System.arraycopy(packetData, i * 200, block, 0, 200);

                    dataToEncrypt.add(block);
                }

                if (remainingBytes > 0) {
                    byte[] finalBlock = new byte[remainingBytes];
                    System.arraycopy(packetData, blocks * 200, finalBlock, 0, remainingBytes);

                    dataToEncrypt.add(finalBlock);
                }
            }

            int position = 1;
            for (byte[] target : dataToEncrypt) {
                try {
                    Cipher cipher = Cipher.getInstance("RSA");
                    cipher.init(Cipher.ENCRYPT_MODE, key);
                    byte[] encrypted = cipher.doFinal(target);

                    CFramePacket frame = new CFramePacket(outgoing.id(), position++, dataToEncrypt.size(), encrypted, outgoing.timestamp());
                    String serial = StringUtils.serialize(frame);
                    byte[] serialData = serial.getBytes();

                    DefinedPacket message = new PluginMessage(prefix + ":" + id, serialData, false);
                    packetList.add(message);
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException ignored) {
                }
            }
        }

        return packetList.toArray(new DefinedPacket[0]);
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
