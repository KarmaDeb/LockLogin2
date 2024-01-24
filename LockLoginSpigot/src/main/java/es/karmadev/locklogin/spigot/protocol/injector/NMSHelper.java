package es.karmadev.locklogin.spigot.protocol.injector;

import es.karmadev.api.spigot.server.SpigotServer;
import es.karmadev.api.strings.StringUtils;
import es.karmadev.locklogin.api.CurrentPlugin;
import es.karmadev.locklogin.api.network.communication.packet.OutgoingPacket;
import es.karmadev.locklogin.common.api.packet.frame.CFramePacket;
import es.karmadev.locklogin.spigot.LockLoginSpigot;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * LockLogin NMS helper
 */
public class NMSHelper {

    private final static LockLoginSpigot plugin = (LockLoginSpigot) CurrentPlugin.getPlugin();
    private final static Class<?> packetDataSerializer = SpigotServer.netMinecraftServer("PacketDataSerializer").orElseGet(() ->
            SpigotServer.netMinecraftServer("PacketDataSerializer", "network").orElse(null)
    );
    private final static Class<?> payloadPacketClass = SpigotServer.netMinecraftServer("PacketPlayOutCustomPayload").orElseGet(() ->
            SpigotServer.netMinecraftServer("PacketPlayOutCustomPayload", "network", "protocol", "game").orElseGet(() ->
                    SpigotServer.netMinecraftServer("ClientboundCustomPayloadPacket", "network", "protocol", "common").orElse(null))
    );
    private final static Class<?> minecraftKeyClass = SpigotServer.netMinecraftServer("MinecraftKey").orElseGet(() ->
            SpigotServer.netMinecraftServer("MinecraftKey", "resources").orElse(null)
    );
    private final static Class<?> customPacketPayload = SpigotServer.netMinecraftServer("CustomPacketPayload",
            "network", "protocol", "common", "custom").orElse(null);

    private static Method sendPacketMethod;

    private final static Class<?> unknownPayloadClass;
    static {
        Class<?> val = null;
        try {
            val = Class.forName("net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket$UnknownPayload");
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}

        unknownPayloadClass = val;
    }

    public static Object createPayload(final byte[] payloadData, final String identifier) {
        if (packetDataSerializer == null || payloadPacketClass == null) return null;

        Object dataWriter;
        if (unknownPayloadClass != null) {
            dataWriter = Unpooled.wrappedBuffer(payloadData);
        } else {
            dataWriter = createPacketData(payloadData);
        }

        Object internalPacket = null;
        if (minecraftKeyClass == null) {
            try {
                Constructor<?> legacyPacketConstructor = payloadPacketClass.getDeclaredConstructor(String.class, packetDataSerializer);
                internalPacket = legacyPacketConstructor.newInstance(identifier, dataWriter);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException ex) {
                ex.printStackTrace();
            }
        } else {
            Object minecraftKey;
            try {
                Constructor<?> keyConstructor = minecraftKeyClass.getDeclaredConstructor(String.class);
                minecraftKey = keyConstructor.newInstance(identifier);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException ex) {
                ex.printStackTrace();
                return new Object[0];
            }

            if (unknownPayloadClass == null) {
                try {
                    Constructor<?> modernPacketConstructor = payloadPacketClass.getDeclaredConstructor(minecraftKeyClass, packetDataSerializer);
                    internalPacket = modernPacketConstructor.newInstance(minecraftKey, dataWriter);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            } else {
                Object unknownPayload;
                try {
                    Constructor<?> payloadConstructor = unknownPayloadClass.getDeclaredConstructor(minecraftKeyClass, ByteBuf.class);
                    unknownPayload = payloadConstructor.newInstance(minecraftKey, (ByteBuf) dataWriter);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException ex) {
                    ex.printStackTrace();
                    return new Object[0];
                }

                try {
                    Constructor<?> modernPacketConstructor = payloadPacketClass.getDeclaredConstructor(customPacketPayload);
                    internalPacket = modernPacketConstructor.newInstance(unknownPayload);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return internalPacket;
    }

    @NotNull
    public static Object[] createPayloads(final String identifier, final OutgoingPacket packet) {
        if (packetDataSerializer == null || payloadPacketClass == null) return new Object[0];
        List<byte[]> dataToEncrypt = new ArrayList<>();

        PrivateKey sharedSecret = /*plugin.getSharedSecret()*/ null;
        if (sharedSecret != null) {
            packet.addProperty(identifier, Base64.getEncoder().encodeToString(sharedSecret.getEncoded()));
        }

        String raw = StringUtils.serialize(packet);
        byte[] packetData = raw.getBytes(StandardCharsets.UTF_8);

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

        List<Object> packetsData = new ArrayList<>();
        int position = 1;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, Injection.sharedPublic);

            for (byte[] target : dataToEncrypt) {
                byte[] encrypted = cipher.doFinal(target);

                CFramePacket frame = new CFramePacket(packet.id(), position++, dataToEncrypt.size(), encrypted, packet.timestamp());
                String serial = StringUtils.serialize(frame);
                byte[] serialData = serial.getBytes(StandardCharsets.UTF_8);

                if (unknownPayloadClass != null) {
                    packetsData.add(Unpooled.wrappedBuffer(serialData));
                } else {
                    Object data = createPacketData(serialData);
                    packetsData.add(data);
                }
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException ex) {
            ex.printStackTrace();
        }

        List<Object> createdPackets = new ArrayList<>();
        for (Object serializedData : packetsData) {
            Object internalPacket = null;

            if (minecraftKeyClass == null) {
                try {
                    Constructor<?> legacyPacketConstructor = payloadPacketClass.getDeclaredConstructor(String.class, packetDataSerializer);
                    internalPacket = legacyPacketConstructor.newInstance(identifier, serializedData);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            } else {
                Object minecraftKey;
                try {
                    Constructor<?> keyConstructor = minecraftKeyClass.getDeclaredConstructor(String.class);
                    minecraftKey = keyConstructor.newInstance(identifier);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException ex) {
                    ex.printStackTrace();
                    return new Object[0];
                }

                if (unknownPayloadClass == null) {
                    try {
                        Constructor<?> modernPacketConstructor = payloadPacketClass.getDeclaredConstructor(minecraftKeyClass, packetDataSerializer);
                        internalPacket = modernPacketConstructor.newInstance(minecraftKey, serializedData);
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                             InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Object unknownPayload;
                    try {
                        Constructor<?> payloadConstructor = unknownPayloadClass.getDeclaredConstructor(minecraftKeyClass, ByteBuf.class);
                        unknownPayload = payloadConstructor.newInstance(minecraftKey, (ByteBuf) serializedData);
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                             InvocationTargetException ex) {
                        ex.printStackTrace();
                        return new Object[0];
                    }

                    try {
                        Constructor<?> modernPacketConstructor = payloadPacketClass.getDeclaredConstructor(customPacketPayload);
                        internalPacket = modernPacketConstructor.newInstance(unknownPayload);
                    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                             InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (internalPacket != null) {
                createdPackets.add(internalPacket);
            }
        }

        return createdPackets.toArray();
    }

    private static Object createPacketData(final byte[] data) {
        try {
            Constructor<?> packetDataConstructor = packetDataSerializer.getDeclaredConstructor(ByteBuf.class);
            return packetDataConstructor.newInstance(Unpooled.wrappedBuffer(data));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }

    public static void sendPacket(final Player player, final Object packet) throws InvocationTargetException, IllegalAccessException {
        Object entityHandle = Injection.toEntityHandle(player);
        Object playerConnection = Injection.playerConnection(entityHandle);

        if (playerConnection != null) {
            Class<?> packetClass = SpigotServer.netMinecraftServer("Packet").orElseGet(() ->
                SpigotServer.netMinecraftServer("Packet", "network", "protocol").orElse(null));

            Method sendPacketMethod = getMethod(packetClass, playerConnection);
            sendPacketMethod.invoke(playerConnection, packet);
        }
    }

    @NotNull
    private static Method getMethod(Class<?> packetClass, Object playerConnection) {
        if (packetClass == null) throw new RuntimeException("Failed to locate minecraft packet class");

        if (sendPacketMethod == null) {
            Class<?> source = playerConnection.getClass();
            while (source != null) {
                Method[] methods = source.getMethods();
                for (Method method : methods) {
                    Parameter[] parameters = method.getParameters();
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(Void.class) || returnType.equals(void.class)) {
                        if (parameters.length == 1) {
                            Parameter parameter = parameters[0];
                            if (parameter.getType().equals(packetClass)) {
                                sendPacketMethod = method;
                                break;
                            }
                        }
                    }
                }

                source = source.getSuperclass();
            }
        }

        if (sendPacketMethod == null) throw new RuntimeException("Cannot send packet because playerConnection object doesn't have sendPacket method");
        sendPacketMethod.setAccessible(true);
        return sendPacketMethod;
    }
}
