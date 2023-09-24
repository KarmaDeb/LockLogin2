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

    @NotNull
    public static Object[] createPayloads(final String identifier, final OutgoingPacket packet) {
        Class<?> packetDataSerializer = SpigotServer.netMinecraftServer("PacketDataSerializer").orElseGet(() -> {
            try {
                return Class.forName("net.minecraft.network.PacketDataSerializer");
            } catch (ClassNotFoundException ex) {
                return null;
            }
        });
        Class<?> payloadPacketClass = SpigotServer.netMinecraftServer("PacketPlayOutCustomPayload").orElseGet(() -> {
            try {
                return Class.forName("net.minecraft.network.protocol.game.PacketPlayOutCustomPayload");
            } catch (ClassNotFoundException ex) {
                return null;
            }
        });
        Class<?> minecraftKeyClass = SpigotServer.netMinecraftServer("MinecraftKey").orElseGet(() -> {
            try {
                return Class.forName("net.minecraft.resources.MinecraftKey");
            } catch (ClassNotFoundException ex) {
                return null;
            }
        });

        if (packetDataSerializer == null || payloadPacketClass == null) return new Object[0];

        //if (packet.getType().equals(DataType.HELLO)) return new Object[]{resolveHello(identifier, packet)};

        List<byte[]> dataToEncrypt = new ArrayList<>();

        PrivateKey sharedSecret = plugin.getSharedSecret();
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
                byte[] serialData = compress(serial.getBytes(StandardCharsets.UTF_8));

                Object data = createPacketData(packetDataSerializer, serialData);
                packetsData.add(data);

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

                try {
                    Constructor<?> modernPacketConstructor = payloadPacketClass.getDeclaredConstructor(minecraftKeyClass, packetDataSerializer);
                    internalPacket = modernPacketConstructor.newInstance(minecraftKey, serializedData);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }

            if (internalPacket != null) {
                createdPackets.add(internalPacket);
            }
        }

        return createdPackets.toArray();
    }

    private static byte[] compress(byte[] input) {
        /*try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArrayOutputStream)) {
            deflaterOutputStream.write(input);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }*/

        return input;
    }

    private static Object createPacketData(final Class<?> clazz, final byte[] data) {
        try {
            Constructor<?> packetDataConstructor = clazz.getDeclaredConstructor(ByteBuf.class);
            return packetDataConstructor.newInstance(Unpooled.wrappedBuffer(data));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }

    public static void sendPacket(final Player player, final Object packet) throws InvocationTargetException, IllegalAccessException {
        Object entityHandle = Injection.toEntityHandle(player);
        Object playerConnection = Injection.playerConnection(entityHandle);

        if (playerConnection != null) {
            Class<?> packetClass = SpigotServer.netMinecraftServer("Packet").orElseGet(() -> {
                try {
                    return Class.forName("net.minecraft.network.protocol.Packet");
                } catch (ClassNotFoundException ex) {
                    return null;
                }
            });

            if (packetClass == null) throw new RuntimeException("Failed to locate minecraft packet class");
            Method[] methods = playerConnection.getClass().getDeclaredMethods();

            Method sendPacketMethod = null;
            for (Method method : methods) {
                Parameter[] parameters = method.getParameters();
                if (parameters.length == 1) {
                    Parameter parameter = parameters[0];
                    if (parameter.getType().equals(packetClass)) {
                        sendPacketMethod = method;
                        break;
                    }
                }
            }

            if (sendPacketMethod == null) throw new RuntimeException("Cannot send packet because playerConnection object doesn't have sendPacket method");
            sendPacketMethod.setAccessible(true);
            sendPacketMethod.invoke(playerConnection, packet);
        }
    }
}
